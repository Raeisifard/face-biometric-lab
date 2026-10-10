package com.isc.face_mobile_demo

import android.content.Context
import android.graphics.Bitmap
import android.os.SystemClock
import androidx.camera.core.ImageProxy
import androidx.camera.core.toBitmap
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.facedetector.FaceDetector
import com.google.mediapipe.tasks.vision.facedetector.FaceDetectorResult
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

data class MediaPipeFace(
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    val confidence: Float,
    val landmarks: List<Float>
)

data class MediaPipeDetectionResult(
    val status: String,
    val faces: List<MediaPipeFace>,
    val imageWidth: Int,
    val imageHeight: Int,
    val processingMs: Long
)

/**
 * LIVE_STREAM is asynchronous. Only one frame is submitted at a time so
 * ignored frames cannot leak their Bitmap while the detector is busy.
 */
class MediaPipeFaceDetector(
    context: Context,
    private val onResult: (MediaPipeDetectionResult, Long) -> Unit,
    private val onError: (Throwable) -> Unit
) : AutoCloseable {
    companion object {
        const val MODEL_ID = "mediapipe-blazeface-short-range"
        const val MODEL_ASSET = "models/face_detection_short_range.tflite"
    }

    private data class FrameMetadata(
        val sequence: Long,
        val width: Int,
        val height: Int,
        val startedNanos: Long,
        val bitmap: Bitmap
    )

    private val closed = AtomicBoolean(false)
    private val inFlight = AtomicBoolean(false)
    private val pending = ConcurrentHashMap<Long, FrameMetadata>()
    private var lastTimestampMs = 0L
    private val detector: FaceDetector

    init {
        val options = FaceDetector.FaceDetectorOptions.builder()
            .setBaseOptions(BaseOptions.builder().setModelAssetPath(MODEL_ASSET).build())
            .setRunningMode(RunningMode.LIVE_STREAM)
            .setMinDetectionConfidence(0.65f)
            .setMinSuppressionThreshold(0.3f)
            .setResultListener { result: FaceDetectorResult, inputImage ->
                handleResult(result, inputImage.timestampMs())
            }
            .setErrorListener { error ->
                pending.entries.firstOrNull()?.let { entry ->
                    if (pending.remove(entry.key, entry.value)) entry.value.bitmap.recycle()
                }
                inFlight.set(false)
                onError(error)
            }
            .build()
        detector = FaceDetector.createFromOptions(context, options)
    }

    @Synchronized
    fun detect(image: ImageProxy, sequence: Long) {
        if (closed.get() || !inFlight.compareAndSet(false, true)) return
        var bitmap: Bitmap? = null
        try {
            bitmap = image.toBitmap()
            val timestamp = maxOf(SystemClock.uptimeMillis(), lastTimestampMs + 1L)
            lastTimestampMs = timestamp
            pending[timestamp] = FrameMetadata(
                sequence = sequence,
                width = bitmap.width,
                height = bitmap.height,
                startedNanos = System.nanoTime(),
                bitmap = bitmap
            )
            detector.detectAsync(BitmapImageBuilder(bitmap).build(), timestamp)
        } catch (error: Throwable) {
            bitmap?.let { created ->
                val entry = pending.entries.firstOrNull { it.value.bitmap === created }
                if (entry != null) pending.remove(entry.key)?.bitmap?.recycle() else created.recycle()
            }
            inFlight.set(false)
            onError(error)
        }
    }

    private fun handleResult(result: FaceDetectorResult, timestampMs: Long) {
        val metadata = pending.remove(timestampMs)
        try {
            if (metadata == null || closed.get()) return
            val faces = result.detections().mapNotNull { detection ->
                val box = detection.boundingBox()
                if (box.width() <= 0f || box.height() <= 0f) return@mapNotNull null
                val landmarks = detection.keypoints().flatMap { point ->
                    listOf(point.x() * metadata.width, point.y() * metadata.height)
                }
                MediaPipeFace(
                    x = box.left,
                    y = box.top,
                    width = box.width(),
                    height = box.height(),
                    confidence = detection.categories().firstOrNull()?.score() ?: 0f,
                    landmarks = landmarks
                )
            }
            val status = when (faces.size) {
                0 -> "NO_FACE"
                1 -> "SINGLE_FACE"
                else -> "MULTIPLE_FACES"
            }
            onResult(
                MediaPipeDetectionResult(
                    status = status,
                    faces = faces,
                    imageWidth = metadata.width,
                    imageHeight = metadata.height,
                    processingMs = (System.nanoTime() - metadata.startedNanos) / 1_000_000L
                ),
                metadata.sequence
            )
        } catch (error: Throwable) {
            onError(error)
        } finally {
            metadata?.bitmap?.recycle()
            inFlight.set(false)
        }
    }

    override fun close() {
        if (!closed.compareAndSet(false, true)) return
        pending.values.forEach { it.bitmap.recycle() }
        pending.clear()
        detector.close()
    }
}

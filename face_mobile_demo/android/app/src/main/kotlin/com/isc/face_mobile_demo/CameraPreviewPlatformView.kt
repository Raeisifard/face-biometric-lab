package com.isc.face_mobile_demo

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.util.Size
import android.view.Surface
import android.view.View
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.platform.PlatformView
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

class CameraPreviewPlatformView(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val requestCameraPermission: () -> Unit,
    private val events: () -> EventChannel.EventSink?
) : PlatformView {

    private val previewView = PreviewView(context).apply {
        // Flutter embeds this PreviewView inside a bounded AndroidView. Use the
        // TextureView-backed implementation so the camera surface obeys the
        // Flutter widget bounds and clips correctly in hybrid/platform-view
        // composition. SurfaceView-backed PERFORMANCE mode can escape those
        // bounds on some Android devices/emulators.
        implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        scaleType = PreviewView.ScaleType.FIT_CENTER
        clipToOutline = true
    }
    private val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private val detectorExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    @Volatile
    private var yuNetDetector: YuNetDetector? = null
    @Volatile
    private var detectorInitializationStarted = false
    private var lastDetectionNanos = 0L
    private val mainHandler = Handler(Looper.getMainLooper())
    private val released = AtomicBoolean(false)
    private val frameCount = AtomicLong(0)
    private var cameraProvider: ProcessCameraProvider? = null
    private var analysis: ImageAnalysis? = null

    init {
        startWhenReady()
    }

    override fun getView(): View = previewView

    private fun emit(block: (EventChannel.EventSink) -> Unit) {
        mainHandler.post {
            if (!released.get()) events()?.let(block)
        }
    }

    private fun startWhenReady() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestCameraPermission()
            emit { it.success(mapOf("type" to "permission_required")) }
            return
        }

        initializeDetectorAsync()
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            if (released.get()) return@addListener
            try {
                cameraProvider = future.get()
                bindUseCases()
            } catch (error: Exception) {
                emit { it.error("CAMERA_INIT_FAILED", error.message, null) }
            }
        }, ContextCompat.getMainExecutor(context))
    }

    @Synchronized
    private fun initializeDetectorAsync() {
        if (detectorInitializationStarted || yuNetDetector != null) return
        detectorInitializationStarted = true

        detectorExecutor.execute {
            try {
                val detector = YuNetDetector(context)
                if (released.get()) {
                    detector.close()
                    return@execute
                }
                yuNetDetector = detector
                emit { it.success(mapOf("type" to "detector_ready", "modelId" to YuNetDetector.MODEL_ID)) }
            } catch (error: Exception) {
                emit {
                    it.error(
                        "FACE_DETECTOR_UNAVAILABLE",
                        error.message ?: "YuNet detector initialization failed",
                        null
                    )
                }
            }
        }
    }

    private fun bindUseCases() {
        val provider = cameraProvider ?: return
        provider.unbindAll()

        val rotation = previewView.display?.rotation ?: Surface.ROTATION_0

        val preview = Preview.Builder()
            .setTargetResolution(Size(1280, 720))
            .setTargetRotation(rotation)
            .build()
            .also { it.setSurfaceProvider(previewView.surfaceProvider) }

        val imageAnalysis = ImageAnalysis.Builder()
            .setTargetResolution(Size(1280, 720))
            .setTargetRotation(rotation)
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setOutputImageRotationEnabled(true)
            .build()

        imageAnalysis.setAnalyzer(cameraExecutor) { image ->
            try {
                if (!released.get()) {
                    val count = frameCount.incrementAndGet()
                    if (count % 5L == 0L) {
                        emit {
                            it.success(
                                mapOf(
                                    "type" to "frame",
                                    "sequence" to count,
                                    "timestamp" to image.imageInfo.timestamp,
                                    "width" to image.width,
                                    "height" to image.height,
                                    "rotationDegrees" to image.imageInfo.rotationDegrees
                                )
                            )
                        }
                    }
                    val now = System.nanoTime()
                    if (now - lastDetectionNanos >= 200_000_000L) {
                        lastDetectionNanos = now
                        val detector = yuNetDetector
                        if (detector != null) {
                            val detection = detector.detect(image)
                            emit { it.success(detectionEvent(detection, count)) }
                        }
                    }
                }
            } catch (error: Exception) {
                emit { it.error("FACE_DETECTION_FAILED", error.message, null) }
            } finally {
                image.close()
            }
        }

        analysis = imageAnalysis

        try {
            provider.bindToLifecycle(
                lifecycleOwner,
                CameraSelector.DEFAULT_FRONT_CAMERA,
                preview,
                imageAnalysis
            )
            emit { it.success(mapOf("type" to "ready")) }
        } catch (error: Exception) {
            emit { it.error("CAMERA_BIND_FAILED", error.message, null) }
        }
    }

    fun restart() {
        if (!released.get()) startWhenReady()
    }

    fun updateRotation() {
        if (!released.get() &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            bindUseCases()
        }
    }

    private fun detectionEvent(result: YuNetResult, sequence: Long): Map<String, Any> {
        val faces = result.faces.map { face ->
            mapOf(
                "x" to face.x,
                "y" to face.y,
                "width" to face.width,
                "height" to face.height,
                "confidence" to face.confidence,
                "landmarks" to face.landmarks.toList()
            )
        }
        return mapOf(
            "type" to "detection",
            "sequence" to sequence,
            "modelId" to YuNetDetector.MODEL_ID,
            "status" to result.status.name,
            "imageWidth" to result.imageWidth,
            "imageHeight" to result.imageHeight,
            "processingMs" to result.processingMs,
            "faces" to faces
        )
    }

    override fun dispose() {
        if (!released.compareAndSet(false, true)) return
        analysis?.clearAnalyzer()
        cameraProvider?.unbindAll()
        yuNetDetector?.close()
        yuNetDetector = null
        cameraExecutor.shutdown()
        detectorExecutor.shutdownNow()
        mainHandler.removeCallbacksAndMessages(null)
    }
}

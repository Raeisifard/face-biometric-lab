package com.isc.face_mobile_demo

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.content.Context
import android.graphics.ImageFormat
import android.media.Image
import androidx.camera.core.ImageProxy
import java.lang.reflect.Array
import java.nio.ByteBuffer
import java.nio.FloatBuffer
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

data class YuNetDetection(
    val x: Float, val y: Float, val width: Float, val height: Float,
    val landmarks: FloatArray, val confidence: Float
)

data class YuNetResult(
    val status: Status, val faces: List<YuNetDetection>,
    val imageWidth: Int, val imageHeight: Int, val processingMs: Long
) {
    enum class Status { NO_FACE, SINGLE_FACE, MULTIPLE_FACES }
}

class YuNetDetector(
    context: Context,
    private val confidenceThreshold: Float = 0.8f,
    private val nmsThreshold: Float = 0.3f,
    private val topK: Int = 5000
) : AutoCloseable {
    companion object {
        const val MODEL_ASSET = "models/face_detection_yunet_2023mar.onnx"
        const val MODEL_ID = "yunet-2023mar"
        private const val INPUT_WIDTH = 640
        private const val INPUT_HEIGHT = 640
        private val STRIDES = intArrayOf(8, 16, 32)
        private val REQUIRED_OUTPUTS = setOf(
            "cls_8", "cls_16", "cls_32", "obj_8", "obj_16", "obj_32",
            "bbox_8", "bbox_16", "bbox_32", "kps_8", "kps_16", "kps_32"
        )
    }

    private val environment = OrtEnvironment.getEnvironment()
    private val session: OrtSession

    init {
        val modelBytes = context.assets.open(MODEL_ASSET).use { it.readBytes() }
        session = environment.createSession(modelBytes, OrtSession.SessionOptions())
        check(session.inputNames.isNotEmpty()) { "YuNet model has no input" }
        check(session.outputNames.containsAll(REQUIRED_OUTPUTS)) {
            "Unexpected YuNet outputs: " + session.outputNames
        }
    }

    fun detect(imageProxy: ImageProxy): YuNetResult {
        val started = System.nanoTime()
        val image = imageProxy.image ?: return emptyResult(started)
        require(image.format == ImageFormat.YUV_420_888) {
            "YuNet expects YUV_420_888, got " + image.format
        }
        val sourceWidth = image.width
        val sourceHeight = image.height
        // Preserve the camera frame aspect ratio. Stretching a landscape or
        // portrait frame directly to 320x320 distorts faces and hurts detection.
        val scale = min(
            INPUT_WIDTH.toFloat() / sourceWidth,
            INPUT_HEIGHT.toFloat() / sourceHeight
        )
        val resizedWidth = (sourceWidth * scale).toInt().coerceIn(1, INPUT_WIDTH)
        val resizedHeight = (sourceHeight * scale).toInt().coerceIn(1, INPUT_HEIGHT)
        val padLeft = (INPUT_WIDTH - resizedWidth) / 2
        val padTop = (INPUT_HEIGHT - resizedHeight) / 2
        val input = yuvToBgrTensor(image, scale, resizedWidth, resizedHeight, padLeft, padTop)
        val tensor = OnnxTensor.createTensor(
            environment, FloatBuffer.wrap(input),
            longArrayOf(1, 3, INPUT_HEIGHT.toLong(), INPUT_WIDTH.toLong())
        )
        try {
            session.run(mapOf(session.inputNames.first() to tensor)).use { outputs ->
                val values = session.outputNames.associateWith { name ->
                    flatten(outputs.get(session.outputNames.indexOf(name)).value)
                }
                val candidates = decode(values, sourceWidth, sourceHeight, scale, padLeft, padTop)
                val selected = nms(candidates)
                val status = when (selected.size) {
                    0 -> YuNetResult.Status.NO_FACE
                    1 -> YuNetResult.Status.SINGLE_FACE
                    else -> YuNetResult.Status.MULTIPLE_FACES
                }
                return YuNetResult(
                    status, selected, sourceWidth, sourceHeight,
                    (System.nanoTime() - started) / 1_000_000
                )
            }
        } finally {
            tensor.close()
        }
    }

    private fun decode(
        values: Map<String, FloatArray>,
        sourceWidth: Int,
        sourceHeight: Int,
        scale: Float,
        padLeft: Int,
        padTop: Int
    ): List<YuNetDetection> {
        val result = mutableListOf<YuNetDetection>()
        for (stride in STRIDES) {
            val cols = INPUT_WIDTH / stride
            val rows = INPUT_HEIGHT / stride
            val suffix = stride.toString()
            val cls = values["cls_" + suffix] ?: error("Missing cls_" + suffix)
            val obj = values["obj_" + suffix] ?: error("Missing obj_" + suffix)
            val bbox = values["bbox_" + suffix] ?: error("Missing bbox_" + suffix)
            val kps = values["kps_" + suffix] ?: error("Missing kps_" + suffix)
            for (r in 0 until rows) for (c in 0 until cols) {
                val idx = r * cols + c
                if (idx >= cls.size || idx >= obj.size || idx * 4 + 3 >= bbox.size || idx * 10 + 9 >= kps.size) continue
                val score = sqrt(cls[idx].coerceIn(0f, 1f) * obj[idx].coerceIn(0f, 1f))
                if (score < confidenceThreshold) continue
                // YuNet bbox outputs are left/top/right/bottom distances from
                // the feature-map cell, not center offsets plus log(width/height).
                val bi = idx * 4
                val left = (c - bbox[bi]) * stride
                val top = (r - bbox[bi + 1]) * stride
                val right = (c + bbox[bi + 2]) * stride
                val bottom = (r + bbox[bi + 3]) * stride
                val li = idx * 10
                val landmarks = FloatArray(10)
                for (n in 0 until 5) {
                    landmarks[n * 2] = (
                        ((kps[li + n * 2] + c) * stride - padLeft) / scale
                    ).coerceIn(0f, sourceWidth.toFloat())
                    landmarks[n * 2 + 1] = (
                        ((kps[li + n * 2 + 1] + r) * stride - padTop) / scale
                    ).coerceIn(0f, sourceHeight.toFloat())
                }
                val sourceLeft = ((left - padLeft) / scale).coerceIn(0f, sourceWidth.toFloat())
                val sourceTop = ((top - padTop) / scale).coerceIn(0f, sourceHeight.toFloat())
                val sourceRight = ((right - padLeft) / scale).coerceIn(0f, sourceWidth.toFloat())
                val sourceBottom = ((bottom - padTop) / scale).coerceIn(0f, sourceHeight.toFloat())
                val boxWidth = sourceRight - sourceLeft
                val boxHeight = sourceBottom - sourceTop
                if (boxWidth <= 0f || boxHeight <= 0f) continue
                result += YuNetDetection(
                    sourceLeft, sourceTop, boxWidth, boxHeight, landmarks, score
                )
            }
        }
        return result
    }

    private fun nms(input: List<YuNetDetection>): List<YuNetDetection> {
        val sorted = input.sortedByDescending { it.confidence }.take(topK).toMutableList()
        val selected = mutableListOf<YuNetDetection>()
        while (sorted.isNotEmpty()) {
            val current = sorted.removeAt(0)
            selected += current
            sorted.removeAll { candidate ->
                // IoU is the usual NMS metric, but it can fail to remove a
                // small duplicate box fully contained inside a larger box.
                // Intersection-over-smaller-area handles that case without
                // merging genuinely separate faces.
                iou(current, candidate) >= nmsThreshold ||
                    intersectionOverSmaller(current, candidate) >= 0.80f
            }
        }
        return selected
    }

    private fun intersectionOverSmaller(a: YuNetDetection, b: YuNetDetection): Float {
        val left = max(a.x, b.x)
        val top = max(a.y, b.y)
        val right = min(a.x + a.width, b.x + b.width)
        val bottom = min(a.y + a.height, b.y + b.height)
        val intersection = max(0f, right - left) * max(0f, bottom - top)
        val smallerArea = min(a.width * a.height, b.width * b.height)
        return if (smallerArea <= 0f) 0f else intersection / smallerArea
    }

    private fun iou(a: YuNetDetection, b: YuNetDetection): Float {
        val left = max(a.x, b.x)
        val top = max(a.y, b.y)
        val right = min(a.x + a.width, b.x + b.width)
        val bottom = min(a.y + a.height, b.y + b.height)
        val intersection = max(0f, right - left) * max(0f, bottom - top)
        val union = a.width * a.height + b.width * b.height - intersection
        return if (union <= 0f) 0f else intersection / union
    }

    private fun yuvToBgrTensor(
        image: Image,
        scale: Float,
        resizedWidth: Int,
        resizedHeight: Int,
        padLeft: Int,
        padTop: Int
    ): FloatArray {
        val targetWidth = INPUT_WIDTH
        val targetHeight = INPUT_HEIGHT
        val planes = image.planes
        val yPlane = planes[0]
        val uPlane = planes[1]
        val vPlane = planes[2]
        val yBuffer = yPlane.buffer
        val uBuffer = uPlane.buffer
        val vBuffer = vPlane.buffer
        val output = FloatArray(3 * targetWidth * targetHeight)
        val planeSize = targetWidth * targetHeight

        fun sample(plane: Image.Plane, buffer: ByteBuffer, x: Int, y: Int): Int {
            val px = min(x, image.width - 1)
            val py = min(y, image.height - 1)
            val index = py * plane.rowStride + px * plane.pixelStride
            return buffer.get(index).toInt() and 0xff
        }

        var offset = 0
        for (y in 0 until targetHeight) {
            for (x in 0 until targetWidth) {
                val insideImage = x >= padLeft && x < padLeft + resizedWidth &&
                    y >= padTop && y < padTop + resizedHeight
                if (!insideImage) {
                    // Black letterbox padding; do not stretch the source frame.
                    output[offset] = 0f
                    output[planeSize + offset] = 0f
                    output[2 * planeSize + offset] = 0f
                    offset++
                    continue
                }
                val sx = (((x - padLeft) / scale).toInt()).coerceIn(0, image.width - 1)
                val sy = (((y - padTop) / scale).toInt()).coerceIn(0, image.height - 1)
                val yy = sample(yPlane, yBuffer, sx, sy)
                val uu = sample(uPlane, uBuffer, sx / 2, sy / 2) - 128
                val vv = sample(vPlane, vBuffer, sx / 2, sy / 2) - 128
                output[offset] = (yy + 1.772f * uu).coerceIn(0f, 255f)
                output[planeSize + offset] =
                    (yy - 0.344136f * uu - 0.714136f * vv).coerceIn(0f, 255f)
                output[2 * planeSize + offset] =
                    (yy + 1.402f * vv).coerceIn(0f, 255f)
                offset++
            }
        }
        return output
    }

    private fun emptyResult(started: Long) = YuNetResult(
        YuNetResult.Status.NO_FACE, emptyList(), 0, 0,
        (System.nanoTime() - started) / 1_000_000
    )

    private fun flatten(value: Any): FloatArray {
        if (value is FloatArray) return value
        val result = FloatArray(totalElements(value))
        var cursor = 0
        fun walk(current: Any) {
            if (current is FloatArray) {
                current.copyInto(result, cursor)
                cursor += current.size
            } else {
                for (i in 0 until Array.getLength(current)) walk(Array.get(current, i))
            }
        }
        walk(value)
        return result
    }

    private fun totalElements(value: Any): Int {
        if (value is FloatArray) return value.size
        var total = 0
        for (i in 0 until Array.getLength(value)) total += totalElements(Array.get(value, i))
        return total
    }

    override fun close() {
        session.close()
    }
}

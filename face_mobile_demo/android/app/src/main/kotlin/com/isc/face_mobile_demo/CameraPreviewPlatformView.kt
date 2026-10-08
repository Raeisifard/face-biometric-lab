package com.isc.face_mobile_demo

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
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
    private val events: EventChannel.EventSink?
) : PlatformView {

    private val previewView = PreviewView(context).apply {
        implementationMode = PreviewView.ImplementationMode.PERFORMANCE
        scaleType = PreviewView.ScaleType.FILL_CENTER
    }
    private val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private val released = AtomicBoolean(false)
    private val frameCount = AtomicLong(0)
    private var cameraProvider: ProcessCameraProvider? = null
    private var analysis: ImageAnalysis? = null

    init {
        startWhenReady()
    }

    override fun getView(): View = previewView

    private fun startWhenReady() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestCameraPermission()
            events?.success(mapOf("type" to "permission_required"))
            return
        }

        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            if (released.get()) return@addListener
            try {
                cameraProvider = future.get()
                bindUseCases()
            } catch (error: Exception) {
                events?.error("CAMERA_INIT_FAILED", error.message, null)
            }
        }, ContextCompat.getMainExecutor(context))
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
            if (!released.get()) {
                val count = frameCount.incrementAndGet()
                if (count % 5L == 0L) {
                    events?.success(
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
            image.close()
        }

        analysis = imageAnalysis

        try {
            provider.bindToLifecycle(
                lifecycleOwner,
                CameraSelector.DEFAULT_FRONT_CAMERA,
                preview,
                imageAnalysis
            )
            events?.success(mapOf("type" to "ready"))
        } catch (error: Exception) {
            events?.error("CAMERA_BIND_FAILED", error.message, null)
        }
    }

    fun restart() {
        if (!released.get()) startWhenReady()
    }

    override fun dispose() {
        if (!released.compareAndSet(false, true)) return
        analysis?.clearAnalyzer()
        cameraProvider?.unbindAll()
        cameraExecutor.shutdown()
        previewView.controller = null
    }
}

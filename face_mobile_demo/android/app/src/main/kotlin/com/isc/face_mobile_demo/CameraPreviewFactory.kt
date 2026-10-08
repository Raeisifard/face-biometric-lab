package com.isc.face_mobile_demo

import android.app.Activity
import android.view.View
import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.platform.PlatformView
import io.flutter.plugin.platform.PlatformViewFactory
import io.flutter.plugin.common.StandardMessageCodec

class CameraPreviewFactory(
    private val activity: Activity,
    private val messenger: BinaryMessenger
) : PlatformViewFactory(StandardMessageCodec.INSTANCE) {

    private var eventSink: EventChannel.EventSink? = null
    private var latestView: CameraPreviewPlatformView? = null

    init {
        EventChannel(messenger, EVENT_CHANNEL).setStreamHandler(object : EventChannel.StreamHandler {
            override fun onListen(arguments: Any?, events: EventChannel.EventSink?) {
                eventSink = events
            }

            override fun onCancel(arguments: Any?) {
                eventSink = null
            }
        })
    }

    override fun create(context: android.content.Context, viewId: Int, args: Any?): PlatformView {
        return CameraPreviewPlatformView(
            context = context,
            lifecycleOwner = activity as androidx.lifecycle.LifecycleOwner,
            requestCameraPermission = {
                activity.requestPermissions(arrayOf(android.Manifest.permission.CAMERA), CAMERA_PERMISSION_REQUEST)
            },
            events = eventSink
        ).also { latestView = it }
    }

    fun restartCamera() {
        latestView?.restart()
    }

    fun updateRotation() {
        latestView?.updateRotation()
    }

    companion object {
        const val VIEW_TYPE = "face_mobile_demo/camera_preview"
        const val EVENT_CHANNEL = "face_mobile_demo/camera_events"
        const val CAMERA_PERMISSION_REQUEST = 3003
    }
}

package com.isc.face_mobile_demo

import android.os.Bundle
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel

class MainActivity : FlutterActivity() {
    companion object {
        private const val CHANNEL = "com.isc.face_mobile_demo/platform"
        private const val BRIDGE_VERSION = "1.1"
    }

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)

        flutterEngine
            .platformViewsController
            .registry
            .registerViewFactory(
                CameraPreviewFactory.VIEW_TYPE,
                CameraPreviewFactory(this, flutterEngine.dartExecutor.binaryMessenger)
            )

        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, CHANNEL)
            .setMethodCallHandler { call, result ->
                when (call.method) {
                    "getHealth" -> result.success(
                        mapOf(
                            "apiName" to "platform.health",
                            "bridgeVersion" to BRIDGE_VERSION,
                            "status" to "OK",
                            "platform" to "Android",
                            "engineState" to "CAMERA_X"
                        )
                    )
                    else -> result.notImplemented()
                }
            }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CameraPreviewFactory.CAMERA_PERMISSION_REQUEST &&
            grantResults.firstOrNull() == android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            recreate()
        }
    }
}

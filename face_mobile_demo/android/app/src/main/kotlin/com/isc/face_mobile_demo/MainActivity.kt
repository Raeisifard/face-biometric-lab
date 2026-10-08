package com.isc.face_mobile_demo

import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel

class MainActivity : FlutterActivity() {
    companion object {
        private const val CHANNEL = "com.isc.face_mobile_demo/platform"
        private const val BRIDGE_VERSION = "1.0"
    }

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)

        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, CHANNEL)
            .setMethodCallHandler { call, result ->
                when (call.method) {
                    "getHealth" -> result.success(
                        mapOf(
                            "apiName" to "platform.health",
                            "bridgeVersion" to BRIDGE_VERSION,
                            "status" to "OK",
                            "platform" to "Android",
                            "engineState" to "PLACEHOLDER"
                        )
                    )
                    else -> result.notImplemented()
                }
            }
    }
}

import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../core/platform/camera_api.dart';

class CameraPreviewPanel extends StatelessWidget {
  const CameraPreviewPanel({super.key});

  @override
  Widget build(BuildContext context) {
    if (defaultTargetPlatform != TargetPlatform.android) {
      return const ColoredBox(
        color: Colors.black,
        child: Center(
          child: Text(
            'CameraX preview is available on Android.',
            style: TextStyle(color: Colors.white),
          ),
        ),
      );
    }

    return const AndroidView(
      viewType: CameraApi.viewType,
      creationParamsCodec: StandardMessageCodec(),
    );
  }
}

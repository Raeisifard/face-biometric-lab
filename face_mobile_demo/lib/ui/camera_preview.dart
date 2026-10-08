import 'dart:async';

import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../core/platform/camera_api.dart';

class CameraPreviewPanel extends StatefulWidget {
  const CameraPreviewPanel({super.key});

  @override
  State<CameraPreviewPanel> createState() => _CameraPreviewPanelState();
}

class _CameraPreviewPanelState extends State<CameraPreviewPanel> {
  final CameraApi _cameraApi = CameraApi();
  StreamSubscription<FaceDetectionEvent>? _subscription;
  FaceDetectionEvent? _detection;

  @override
  void initState() {
    super.initState();
    if (defaultTargetPlatform == TargetPlatform.android) {
      _subscription = _cameraApi.detections.listen((event) {
        if (mounted) setState(() => _detection = event);
      });
    }
  }

  @override
  void dispose() {
    _subscription?.cancel();
    super.dispose();
  }

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

    final detection = _detection;
    final label = switch (detection?.status) {
      'SINGLE_FACE' => '1 face detected',
      'MULTIPLE_FACES' => 'Multiple faces detected',
      'NO_FACE' => 'No face detected',
      _ => 'Detecting face…',
    };

    return Stack(
      fit: StackFit.expand,
      children: [
        const AndroidView(
          viewType: CameraApi.viewType,
          creationParamsCodec: StandardMessageCodec(),
        ),
        Positioned(
          left: 12,
          right: 12,
          bottom: 12,
          child: DecoratedBox(
            decoration: BoxDecoration(
              color: Colors.black54,
              borderRadius: BorderRadius.all(Radius.circular(12)),
            ),
            child: Padding(
              padding: EdgeInsets.symmetric(horizontal: 12, vertical: 8),
              child: Text(
                label,
                textAlign: TextAlign.center,
                style: TextStyle(color: Colors.white),
              ),
            ),
          ),
        ),
      ],
    );
  }
}

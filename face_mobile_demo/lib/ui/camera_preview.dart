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
        Positioned.fill(
          child: IgnorePointer(
            child: CustomPaint(
              painter: FaceDetectionOverlayPainter(detection),
            ),
          ),
        ),
        Positioned(
          left: 12,
          right: 12,
          bottom: 12,
          child: DecoratedBox(
            decoration: const BoxDecoration(
              color: Colors.black54,
              borderRadius: BorderRadius.all(Radius.circular(12)),
            ),
            child: Padding(
              padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
              child: Text(
                label,
                textAlign: TextAlign.center,
                style: const TextStyle(color: Colors.white),
              ),
            ),
          ),
        ),
      ],
    );
  }
}

class FaceDetectionOverlayPainter extends CustomPainter {
  FaceDetectionOverlayPainter(this.detection);

  final FaceDetectionEvent? detection;

  @override
  void paint(Canvas canvas, Size size) {
    final event = detection;
    if (event == null || event.faces.isEmpty) return;
    if (event.imageWidth <= 0 || event.imageHeight <= 0) return;

    final sourceSize = Size(
      event.imageWidth.toDouble(),
      event.imageHeight.toDouble(),
    );

    // CameraPreviewPlatformView configures PreviewView.ScaleType.FIT_CENTER.
    // Match that contain transform so boxes align with the visible preview.
    final scale = _fitScale(sourceSize, size);
    final renderedWidth = sourceSize.width * scale;
    final renderedHeight = sourceSize.height * scale;
    final offsetX = (size.width - renderedWidth) / 2;
    final offsetY = (size.height - renderedHeight) / 2;

    final isSingleFace = event.status == 'SINGLE_FACE';
    final paint = Paint()
      ..style = PaintingStyle.stroke
      ..strokeWidth = 3
      ..color = isSingleFace ? Colors.greenAccent : Colors.orangeAccent;

    final labelPaint = Paint()
      ..style = PaintingStyle.fill
      ..color = paint.color;

    for (final face in event.faces) {
      final box = _readBox(face);
      if (box == null || box.width <= 0 || box.height <= 0) continue;

      final rect = Rect.fromLTWH(
        offsetX + box.left * scale,
        offsetY + box.top * scale,
        box.width * scale,
        box.height * scale,
      );

      canvas.drawRect(rect, paint);

      final label = isSingleFace ? 'FACE' : 'FACE';
      final textPainter = TextPainter(
        text: TextSpan(
          text: label,
          style: TextStyle(
            color: labelPaint.color,
            fontSize: 13,
            fontWeight: FontWeight.bold,
          ),
        ),
        textDirection: TextDirection.ltr,
      )..layout();

      final labelOffset = Offset(
        rect.left,
        (rect.top - textPainter.height - 4).clamp(0.0, size.height).toDouble(),
      );
      textPainter.paint(canvas, labelOffset);
    }
  }

  double _fitScale(Size source, Size target) {
    return (target.width / source.width < target.height / source.height)
        ? target.width / source.width
        : target.height / source.height;
  }

  Rect? _readBox(Map<String, dynamic> face) {
    Map<String, dynamic>? box;
    final rawBox = face['box'];
    if (rawBox is Map) {
      box = Map<String, dynamic>.from(rawBox);
    }

    num? number(String key) {
      final value = box?[key] ?? face[key];
      return value is num ? value : num.tryParse(value?.toString() ?? '');
    }

    final x = number('x');
    final y = number('y');
    final width = number('width');
    final height = number('height');

    if (x == null || y == null || width == null || height == null) return null;
    return Rect.fromLTWH(
      x.toDouble(),
      y.toDouble(),
      width.toDouble(),
      height.toDouble(),
    );
  }

  @override
  bool shouldRepaint(covariant FaceDetectionOverlayPainter oldDelegate) {
    return oldDelegate.detection != detection;
  }
}

import 'dart:async';

import 'package:flutter/services.dart';

class CameraFrameEvent {
  const CameraFrameEvent({
    required this.sequence,
    required this.timestamp,
    required this.width,
    required this.height,
    required this.rotationDegrees,
  });

  final int sequence;
  final int timestamp;
  final int width;
  final int height;
  final int rotationDegrees;

  factory CameraFrameEvent.fromMap(Map<Object?, Object?> map) {
    int value(String key) {
      final raw = map[key];
      return raw is num ? raw.toInt() : 0;
    }

    return CameraFrameEvent(
      sequence: value('sequence'),
      timestamp: value('timestamp'),
      width: value('width'),
      height: value('height'),
      rotationDegrees: value('rotationDegrees'),
    );
  }
}

class FaceDetectionEvent {
  const FaceDetectionEvent({
    required this.status,
    required this.modelId,
    required this.imageWidth,
    required this.imageHeight,
    required this.processingMs,
    required this.faces,
  });

  final String status;
  final String modelId;
  final int imageWidth;
  final int imageHeight;
  final int processingMs;
  final List<Map<String, dynamic>> faces;

  int get faceCount => faces.length;

  factory FaceDetectionEvent.fromMap(Map<Object?, Object?> map) {
    final rawFaces = map['faces'];
    final faces = rawFaces is List
        ? rawFaces.whereType<Map>().map(
            (face) => Map<String, dynamic>.from(face),
          ).toList()
        : const <Map<String, dynamic>>[];
    int number(String key) => map[key] is num ? (map[key] as num).toInt() : 0;
    return FaceDetectionEvent(
      status: map['status']?.toString() ?? 'NO_FACE',
      modelId: map['modelId']?.toString() ?? 'unknown',
      imageWidth: number('imageWidth'),
      imageHeight: number('imageHeight'),
      processingMs: number('processingMs'),
      faces: faces,
    );
  }
}

class CameraApi {
  CameraApi({EventChannel? eventChannel})
      : _events = eventChannel ?? const EventChannel(_eventChannelName);

  static const viewType = 'face_mobile_demo/camera_preview';
  static const _eventChannelName = 'face_mobile_demo/camera_events';

  final EventChannel _events;

  Stream<dynamic> get _rawEvents => _events.receiveBroadcastStream();

  Stream<CameraFrameEvent> get frames => _rawEvents
      .where((event) => event is Map && event['type'] == 'frame')
      .map(
        (event) => CameraFrameEvent.fromMap(
          Map<Object?, Object?>.from(event as Map),
        ),
      );

  Stream<FaceDetectionEvent> get detections => _rawEvents
      .where((event) => event is Map && event['type'] == 'detection')
      .map(
        (event) => FaceDetectionEvent.fromMap(
          Map<Object?, Object?>.from(event as Map),
        ),
      );
}

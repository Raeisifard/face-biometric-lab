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

class CameraApi {
  CameraApi({EventChannel? eventChannel})
      : _events = eventChannel ?? const EventChannel(_eventChannelName);

  static const viewType = 'face_mobile_demo/camera_preview';
  static const _eventChannelName = 'face_mobile_demo/camera_events';

  final EventChannel _events;

  Stream<CameraFrameEvent> get frames => _events
      .receiveBroadcastStream()
      .where((event) => event is Map && event['type'] == 'frame')
      .map(
        (event) => CameraFrameEvent.fromMap(
          Map<Object?, Object?>.from(event as Map),
        ),
      );
}

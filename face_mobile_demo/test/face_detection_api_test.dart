import 'package:flutter_test/flutter_test.dart';

import 'package:face_mobile_demo/core/platform/camera_api.dart';

void main() {
  test('parses single-face detection event', () {
    final event = FaceDetectionEvent.fromMap({
      'status': 'SINGLE_FACE',
      'modelId': 'yunet-2023mar',
      'imageWidth': 1280,
      'imageHeight': 720,
      'processingMs': 12,
      'faces': [
        {
          'x': 320.0,
          'y': 120.0,
          'width': 280.0,
          'height': 280.0,
          'confidence': 0.93,
          'landmarks': [1.0, 2.0, 3.0, 4.0],
        },
      ],
    });

    expect(event.status, 'SINGLE_FACE');
    expect(event.faceCount, 1);
    expect(event.modelId, 'yunet-2023mar');
    expect(event.processingMs, 12);
    expect(event.faces.single['confidence'], 0.93);
  });

  test('malformed detection metadata falls back safely', () {
    final event = FaceDetectionEvent.fromMap({
      'status': null,
      'imageWidth': 'bad',
      'faces': 'bad',
    });

    expect(event.status, 'NO_FACE');
    expect(event.imageWidth, 0);
    expect(event.faceCount, 0);
  });
}

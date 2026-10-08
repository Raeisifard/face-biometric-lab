import 'package:flutter_test/flutter_test.dart';
import 'package:face_mobile_demo/core/platform/camera_api.dart';

void main() {
  test('parses bounded camera frame metadata', () {
    final frame = CameraFrameEvent.fromMap({
      'sequence': 10,
      'timestamp': 123456789,
      'width': 1280,
      'height': 720,
      'rotationDegrees': 90,
    });

    expect(frame.sequence, 10);
    expect(frame.timestamp, 123456789);
    expect(frame.width, 1280);
    expect(frame.height, 720);
    expect(frame.rotationDegrees, 90);
  });

  test('defaults malformed numeric metadata to zero', () {
    final frame = CameraFrameEvent.fromMap({
      'sequence': 'not-a-number',
      'timestamp': null,
      'width': 1280.5,
    });

    expect(frame.sequence, 0);
    expect(frame.timestamp, 0);
    expect(frame.width, 1280);
    expect(frame.height, 0);
    expect(frame.rotationDegrees, 0);
  });
}

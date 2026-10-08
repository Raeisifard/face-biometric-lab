import 'package:flutter/services.dart';
import 'platform_health.dart';

abstract interface class PlatformApi {
  Future<PlatformHealth> getHealth();
}

class MethodChannelPlatformApi implements PlatformApi {
  MethodChannelPlatformApi({MethodChannel? channel})
      : _channel = channel ?? const MethodChannel(_channelName);

  static const _channelName = 'com.isc.face_mobile_demo/platform';
  final MethodChannel _channel;

  @override
  Future<PlatformHealth> getHealth() async {
    final result =
        await _channel.invokeMethod<Map<Object?, Object?>>('getHealth');
    if (result == null) {
      throw PlatformException(
        code: 'EMPTY_HEALTH_RESPONSE',
        message: 'Native platform returned no health response.',
      );
    }
    return PlatformHealth.fromMap(result);
  }
}

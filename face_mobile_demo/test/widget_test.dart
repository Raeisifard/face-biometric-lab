import 'package:flutter_test/flutter_test.dart';

import 'package:face_mobile_demo/app.dart';
import 'package:face_mobile_demo/core/config/app_config.dart';
import 'package:face_mobile_demo/core/platform/platform_api.dart';
import 'package:face_mobile_demo/core/platform/platform_health.dart';

class FakePlatformApi implements PlatformApi {
  @override
  Future<PlatformHealth> getHealth() async => const PlatformHealth(
        apiName: 'platform.health',
        bridgeVersion: '1.0',
        status: 'OK',
        platform: 'Android',
        engineState: 'PLACEHOLDER',
      );
}

void main() {
  testWidgets('shows native platform health', (tester) async {
    await tester.pumpWidget(
      const FaceMobileDemoApp(
        platformApi: FakePlatformApi(),
        config: AppConfig(
          environment: 'test',
          serviceBaseUrl: 'http://test',
          demoMode: true,
        ),
      ),
    );
    await tester.pumpAndSettle();

    expect(find.text('Platform bridge ready'), findsOneWidget);
    expect(find.text('platform.health'), findsOneWidget);
    expect(find.text('PLACEHOLDER'), findsOneWidget);
    expect(find.text('test'), findsOneWidget);
  });
}

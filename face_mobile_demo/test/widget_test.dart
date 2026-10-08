import 'package:flutter_test/flutter_test.dart';
import 'package:face_mobile_demo/app.dart';
import 'package:face_mobile_demo/core/config/app_config.dart';
import 'package:face_mobile_demo/core/platform/platform_api.dart';
import 'package:face_mobile_demo/core/platform/platform_health.dart';

class FakePlatformApi implements PlatformApi {
  @override
  Future<PlatformHealth> getHealth() async => const PlatformHealth(apiName: 'platform.health', bridgeVersion: '1.0', status: 'OK', platform: 'Android', engineState: 'PLACEHOLDER');
}

void main() {
  FaceMobileDemoApp buildApp() => FaceMobileDemoApp(
    platformApi: FakePlatformApi(),
    config: const AppConfig(environment: 'test', serviceBaseUrl: 'http://test', demoMode: true),
  );

  testWidgets('renders M02 navigation and verification entry state', (tester) async {
    await tester.pumpWidget(buildApp());
    expect(find.text('Verify Identity'), findsOneWidget);
    expect(find.text('Start verification'), findsOneWidget);
    expect(find.text('Method'), findsOneWidget);
    expect(find.text('Reference'), findsOneWidget);
    expect(find.text('Settings'), findsOneWidget);
    expect(find.text('Help'), findsOneWidget);
  });

  testWidgets('runs deterministic capture to processing to match flow', (tester) async {
    await tester.pumpWidget(buildApp());
    await tester.tap(find.text('Start verification'));
    await tester.pump();
    expect(find.text('Camera placeholder'), findsOneWidget);
    await tester.tap(find.text('Continue'));
    await tester.pump();
    expect(find.text('Processing verification…'), findsOneWidget);
    await tester.tap(find.text('Demo Match'));
    await tester.pump();
    expect(find.text('Match'), findsOneWidget);
    expect(find.text('Verify again'), findsOneWidget);
  });

  testWidgets('allows method and developer mode selection', (tester) async {
    await tester.pumpWidget(buildApp());
    await tester.tap(find.text('Method'));
    await tester.pump();
    expect(find.text('Hybrid Best Frame'), findsOneWidget);
    await tester.tap(find.text('Full Clip'));
    await tester.pump();
    await tester.tap(find.text('Settings'));
    await tester.pump();
    await tester.tap(find.text('Developer'));
    await tester.pump();
    expect(find.text('Face service'), findsOneWidget);
  });
}

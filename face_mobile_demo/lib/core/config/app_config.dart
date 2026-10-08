class AppConfig {
  const AppConfig({
    required this.environment,
    required this.serviceBaseUrl,
    required this.demoMode,
  });

  final String environment;
  final String serviceBaseUrl;
  final bool demoMode;

  static const defaults = AppConfig(
    environment: String.fromEnvironment('APP_ENVIRONMENT', defaultValue: 'dev'),
    serviceBaseUrl: String.fromEnvironment(
      'FACE_SERVICE_BASE_URL',
      defaultValue: 'http://10.0.2.2:8090',
    ),
    demoMode: bool.fromEnvironment('APP_DEMO_MODE', defaultValue: true),
  );
}

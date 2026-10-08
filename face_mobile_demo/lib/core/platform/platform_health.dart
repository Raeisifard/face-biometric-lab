class PlatformHealth {
  const PlatformHealth({
    required this.apiName,
    required this.bridgeVersion,
    required this.status,
    required this.platform,
    required this.engineState,
  });

  final String apiName;
  final String bridgeVersion;
  final String status;
  final String platform;
  final String engineState;

  bool get isHealthy => status == 'OK';

  factory PlatformHealth.fromMap(Map<Object?, Object?> map) {
    String value(String key) => map[key]?.toString() ?? '';
    return PlatformHealth(
      apiName: value('apiName'),
      bridgeVersion: value('bridgeVersion'),
      status: value('status'),
      platform: value('platform'),
      engineState: value('engineState'),
    );
  }
}

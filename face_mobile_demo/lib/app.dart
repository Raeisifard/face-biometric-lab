import 'package:flutter/material.dart';
import 'core/config/app_config.dart';
import 'core/platform/platform_api.dart';
import 'core/platform/platform_health.dart';

class FaceMobileDemoApp extends StatelessWidget {
  const FaceMobileDemoApp({
    super.key,
    this.platformApi,
    this.config = AppConfig.defaults,
  });

  final PlatformApi? platformApi;
  final AppConfig config;

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Face Mobile Demo',
      debugShowCheckedModeBanner: false,
      theme: ThemeData(
        colorScheme: ColorScheme.fromSeed(seedColor: Colors.indigo),
        useMaterial3: true,
      ),
      home: PlatformStatusPage(
        platformApi: platformApi ?? MethodChannelPlatformApi(),
        config: config,
      ),
    );
  }
}

class PlatformStatusPage extends StatefulWidget {
  const PlatformStatusPage({
    super.key,
    required this.platformApi,
    required this.config,
  });

  final PlatformApi platformApi;
  final AppConfig config;

  @override
  State<PlatformStatusPage> createState() => _PlatformStatusPageState();
}

class _PlatformStatusPageState extends State<PlatformStatusPage> {
  PlatformHealth? _health;
  Object? _error;
  bool _loading = true;

  @override
  void initState() {
    super.initState();
    _loadHealth();
  }

  Future<void> _loadHealth() async {
    setState(() {
      _loading = true;
      _error = null;
    });
    try {
      final health = await widget.platformApi.getHealth();
      if (!mounted) return;
      setState(() {
        _health = health;
        _loading = false;
      });
    } catch (error) {
      if (!mounted) return;
      setState(() {
        _error = error;
        _loading = false;
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    final health = _health;
    return Scaffold(
      appBar: AppBar(title: const Text('Face Mobile Demo')),
      body: ListView(
        padding: const EdgeInsets.all(24),
        children: [
          Text('M01 Foundation',
              style: Theme.of(context).textTheme.headlineSmall),
          const SizedBox(height: 8),
          const Text('Flutter presentation + Kotlin platform bridge'),
          const SizedBox(height: 24),
          Card(
            child: Padding(
              padding: const EdgeInsets.all(16),
              child: _loading
                  ? const Center(child: CircularProgressIndicator())
                  : _error != null
                      ? _ErrorView(error: _error!, onRetry: _loadHealth)
                      : _HealthView(health: health!),
            ),
          ),
          const SizedBox(height: 16),
          ListTile(
            title: const Text('Environment'),
            subtitle: Text(widget.config.environment),
          ),
          ListTile(
            title: const Text('Face service'),
            subtitle: Text(widget.config.serviceBaseUrl),
          ),
          ListTile(
            title: const Text('Demo mode'),
            subtitle: Text(widget.config.demoMode ? 'Enabled' : 'Disabled'),
          ),
        ],
      ),
    );
  }
}

class _HealthView extends StatelessWidget {
  const _HealthView({required this.health});
  final PlatformHealth health;

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Row(
          children: [
            Icon(
              health.isHealthy ? Icons.check_circle : Icons.error,
              color: health.isHealthy
                  ? Theme.of(context).colorScheme.primary
                  : Theme.of(context).colorScheme.error,
            ),
            const SizedBox(width: 8),
            Text(
              health.isHealthy ? 'Platform bridge ready' : 'Platform bridge error',
              style: Theme.of(context).textTheme.titleMedium,
            ),
          ],
        ),
        const SizedBox(height: 16),
        _ValueRow(label: 'API', value: health.apiName),
        _ValueRow(label: 'Bridge', value: health.bridgeVersion),
        _ValueRow(label: 'Platform', value: health.platform),
        _ValueRow(label: 'Engine', value: health.engineState),
      ],
    );
  }
}

class _ValueRow extends StatelessWidget {
  const _ValueRow({required this.label, required this.value});
  final String label;
  final String value;

  @override
  Widget build(BuildContext context) => Padding(
        padding: const EdgeInsets.symmetric(vertical: 4),
        child: Row(
          children: [
            SizedBox(width: 110, child: Text(label)),
            Expanded(child: Text(value)),
          ],
        ),
      );
}

class _ErrorView extends StatelessWidget {
  const _ErrorView({required this.error, required this.onRetry});
  final Object error;
  final VoidCallback onRetry;

  @override
  Widget build(BuildContext context) => Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Text('Platform bridge unavailable'),
          const SizedBox(height: 8),
          Text(error.toString()),
          const SizedBox(height: 12),
          FilledButton.icon(
            onPressed: onRetry,
            icon: const Icon(Icons.refresh),
            label: const Text('Retry'),
          ),
        ],
      );
}

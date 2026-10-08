import 'package:flutter/material.dart';
import 'core/config/app_config.dart';
import 'core/platform/platform_api.dart';

enum VerificationState { ready, camera, processing, match, noMatch }
enum VerificationMethod { fullClip, serverLiveStream, clientEmbedding, hybridBestFrame, hybridMultiFrame }
enum ExperienceMode { customer, developer }

extension VerificationMethodX on VerificationMethod {
  String get label => switch (this) {
    VerificationMethod.fullClip => 'Full Clip',
    VerificationMethod.serverLiveStream => 'Server Live Stream',
    VerificationMethod.clientEmbedding => 'Client Embedding',
    VerificationMethod.hybridBestFrame => 'Hybrid Best Frame',
    VerificationMethod.hybridMultiFrame => 'Hybrid Multi Frame',
  };
  String get description => switch (this) {
    VerificationMethod.fullClip => 'Capture a short clip and verify server-side.',
    VerificationMethod.serverLiveStream => 'Stream selected frames for server verification.',
    VerificationMethod.clientEmbedding => 'Generate an embedding on-device, then verify it.',
    VerificationMethod.hybridBestFrame => 'Select the best frame before verification.',
    VerificationMethod.hybridMultiFrame => 'Use multiple strong frames for verification.',
  };
}

class FaceMobileDemoApp extends StatefulWidget {
  const FaceMobileDemoApp({super.key, this.platformApi, this.config = AppConfig.defaults});
  final PlatformApi? platformApi;
  final AppConfig config;

  @override
  State<FaceMobileDemoApp> createState() => _FaceMobileDemoAppState();
}

class _FaceMobileDemoAppState extends State<FaceMobileDemoApp> {
  int index = 0;
  VerificationMethod method = VerificationMethod.hybridBestFrame;
  String reference = 'Demo reference';
  ExperienceMode mode = ExperienceMode.customer;
  VerificationState state = VerificationState.ready;

  @override
  Widget build(BuildContext context) {
    final pages = [
      VerifyPage(state: state, method: method, reference: reference,
          onStart: () => setState(() => state = VerificationState.camera),
          onContinue: () => setState(() => state = VerificationState.processing),
          onMatch: () => setState(() => state = VerificationState.match),
          onNoMatch: () => setState(() => state = VerificationState.noMatch),
          onReset: () => setState(() => state = VerificationState.ready),
          onHelp: () => setState(() => index = 4)),
      MethodsPage(selected: method, onSelected: (v) => setState(() => method = v)),
      ReferencePage(selected: reference, onSelected: (v) => setState(() => reference = v)),
      SettingsPage(config: widget.config, mode: mode, onModeChanged: (v) => setState(() => mode = v)),
      const HelpPage(),
    ];
    return MaterialApp(
      title: 'Face Mobile Demo',
      debugShowCheckedModeBanner: false,
      theme: ThemeData(useMaterial3: true, colorScheme: ColorScheme.fromSeed(seedColor: Colors.indigo)),
      home: Scaffold(
        appBar: AppBar(title: const Text('Face Mobile Demo'), actions: [
          IconButton(tooltip: 'Help', icon: const Icon(Icons.help_outline), onPressed: () => setState(() => index = 4)),
        ]),
        body: IndexedStack(index: index, children: pages),
        bottomNavigationBar: NavigationBar(
          selectedIndex: index,
          onDestinationSelected: (v) => setState(() => index = v),
          destinations: const [
            NavigationDestination(icon: Icon(Icons.face_outlined), label: 'Verify'),
            NavigationDestination(icon: Icon(Icons.tune_outlined), label: 'Method'),
            NavigationDestination(icon: Icon(Icons.badge_outlined), label: 'Reference'),
            NavigationDestination(icon: Icon(Icons.settings_outlined), label: 'Settings'),
            NavigationDestination(icon: Icon(Icons.help_outline), label: 'Help'),
          ],
        ),
      ),
    );
  }
}

class VerifyPage extends StatelessWidget {
  const VerifyPage({super.key, required this.state, required this.method, required this.reference, required this.onStart, required this.onContinue, required this.onMatch, required this.onNoMatch, required this.onReset, required this.onHelp});
  final VerificationState state;
  final VerificationMethod method;
  final String reference;
  final VoidCallback onStart, onContinue, onMatch, onNoMatch, onReset, onHelp;

  @override
  Widget build(BuildContext context) {
    Widget card;
    switch (state) {
      case VerificationState.ready:
        card = Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
          const Icon(Icons.verified_user_outlined, size: 44),
          const SizedBox(height: 12),
          Text('Ready to verify', style: Theme.of(context).textTheme.titleLarge),
          Text('Method: ${method.label}'),
          Text('Reference: $reference'),
          TextButton.icon(onPressed: onHelp, icon: const Icon(Icons.info_outline), label: const Text('How verification works')),
        ]);
      case VerificationState.camera:
        card = Column(children: [
          Container(height: 260, width: double.infinity,
            decoration: BoxDecoration(color: Theme.of(context).colorScheme.surfaceContainerHighest, borderRadius: BorderRadius.circular(20)),
            child: const Column(mainAxisAlignment: MainAxisAlignment.center, children: [
              Icon(Icons.camera_alt_outlined, size: 64), SizedBox(height: 12),
              Text('Camera placeholder'), Text('CameraX is introduced in M03.'),
            ])),
          const SizedBox(height: 16),
          const Text('Center your face inside the guide.'),
          const SizedBox(height: 12),
          FilledButton(onPressed: onContinue, child: const Text('Continue')),
        ]);
      case VerificationState.processing:
        card = Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
          const Row(children: [CircularProgressIndicator(), SizedBox(width: 12), Text('Processing verification…')]),
          const SizedBox(height: 16),
          const Text('M02 uses deterministic controls; no backend or ML is called.'),
          const SizedBox(height: 12),
          OutlinedButton(onPressed: onMatch, child: const Text('Demo Match')),
          OutlinedButton(onPressed: onNoMatch, child: const Text('Demo No Match')),
        ]);
      case VerificationState.match:
      case VerificationState.noMatch:
        final match = state == VerificationState.match;
        card = Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
          Icon(match ? Icons.check_circle : Icons.cancel, size: 56),
          const SizedBox(height: 12),
          Text(match ? 'Match' : 'No Match', style: Theme.of(context).textTheme.headlineSmall),
          Text(match ? 'The selected reference matches the captured identity.' : 'The captured identity does not match the selected reference.'),
          const SizedBox(height: 16),
          OutlinedButton.icon(onPressed: onReset, icon: const Icon(Icons.replay), label: const Text('Verify again')),
        ]);
    }
    return ListView(padding: const EdgeInsets.all(20), children: [
      Text('Verify Identity', style: Theme.of(context).textTheme.headlineMedium),
      const SizedBox(height: 6),
      const Text('Confirm a face against the selected reference.'),
      const SizedBox(height: 16),
      Text('Flow: ${state.name}', style: Theme.of(context).textTheme.labelLarge),
      const SizedBox(height: 12),
      Card(child: Padding(padding: const EdgeInsets.all(20), child: card)),
      if (state == VerificationState.ready) ...[
        const SizedBox(height: 12),
        FilledButton.icon(onPressed: onStart, icon: const Icon(Icons.camera_alt), label: const Text('Start verification')),
      ],
    ]);
  }
}

class MethodsPage extends StatelessWidget {
  const MethodsPage({super.key, required this.selected, required this.onSelected});
  final VerificationMethod selected;
  final ValueChanged<VerificationMethod> onSelected;

  @override
  Widget build(BuildContext context) => ListView(padding: const EdgeInsets.all(20), children: [
    Text('Verification method', style: Theme.of(context).textTheme.headlineSmall),
    const SizedBox(height: 8),
    const Text('Choose a method for the next verification. Execution is introduced in later phases.'),
    RadioGroup<VerificationMethod>(
      groupValue: selected,
      onChanged: (v) {
        if (v != null) onSelected(v);
      },
      child: Column(
        children: [
          for (final m in VerificationMethod.values)
            Card(
              child: RadioListTile<VerificationMethod>(
                value: m,
                title: Text(m.label),
                subtitle: Text(m.description),
              ),
            ),
        ],
      ),
    ),
  ]);
}

class ReferencePage extends StatelessWidget {
  const ReferencePage({super.key, required this.selected, required this.onSelected});
  final String selected;
  final ValueChanged<String> onSelected;
  static const values = ['Demo reference', 'Reference A', 'Reference B'];

  @override
  Widget build(BuildContext context) => ListView(padding: const EdgeInsets.all(20), children: [
    Text('Reference', style: Theme.of(context).textTheme.headlineSmall),
    const SizedBox(height: 8),
    const Text('Select which registered identity the verification flow should target.'),
    RadioGroup<String>(
      groupValue: selected,
      onChanged: (v) {
        if (v != null) onSelected(v);
      },
      child: Column(
        children: [
          for (final r in values)
            Card(
              child: RadioListTile<String>(
                value: r,
                title: Text(r),
                subtitle: Text(
                  r == 'Demo reference'
                      ? 'Safe local demo selection'
                      : 'Placeholder reference',
                ),
              ),
            ),
        ],
      ),
    ),
  ]);
}

class SettingsPage extends StatelessWidget {
  const SettingsPage({super.key, required this.config, required this.mode, required this.onModeChanged});
  final AppConfig config;
  final ExperienceMode mode;
  final ValueChanged<ExperienceMode> onModeChanged;

  @override
  Widget build(BuildContext context) => ListView(padding: const EdgeInsets.all(20), children: [
    Text('Settings', style: Theme.of(context).textTheme.headlineSmall),
    const SizedBox(height: 8),
    const Text('Customer mode keeps the flow focused. Developer mode exposes demo diagnostics.'),
    const SizedBox(height: 16),
    SegmentedButton<ExperienceMode>(
      segments: const [
        ButtonSegment(value: ExperienceMode.customer, label: Text('Customer'), icon: Icon(Icons.person_outline)),
        ButtonSegment(value: ExperienceMode.developer, label: Text('Developer'), icon: Icon(Icons.code)),
      ],
      selected: {mode}, onSelectionChanged: (v) => onModeChanged(v.first),
    ),
    const SizedBox(height: 20),
    Card(child: ListTile(title: const Text('Environment'), subtitle: Text(config.environment))),
    if (mode == ExperienceMode.developer) ...[
      Card(child: ListTile(title: const Text('Face service'), subtitle: Text(config.serviceBaseUrl))),
      const Card(child: ListTile(title: Text('Demo mode'), subtitle: Text('Enabled for deterministic M02 state previews'))),
    ],
  ]);
}

class HelpPage extends StatelessWidget {
  const HelpPage({super.key});

  @override
  Widget build(BuildContext context) => ListView(padding: const EdgeInsets.all(20), children: [
    Text('Help & About', style: Theme.of(context).textTheme.headlineSmall),
    const ExpansionTile(title: Text('Verify Identity'), children: [Padding(padding: EdgeInsets.all(16), child: Text('Select a reference and method, capture the face, then review the deterministic result state.'))]),
    const ExpansionTile(title: Text('Security boundary'), children: [Padding(padding: EdgeInsets.all(16), child: Text('Client-generated biometric evidence is untrusted. Production security controls are introduced in later phases.'))]),
    const ExpansionTile(title: Text('About this demo'), children: [Padding(padding: EdgeInsets.all(16), child: Text('M02 defines the product UI and state model. CameraX, detection, liveness, ML and backend execution are intentionally not part of this phase.'))]),
  ]);
}

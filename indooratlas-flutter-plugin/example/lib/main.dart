import 'package:flutter/material.dart';
import 'package:indooratlas/indooratlas.dart';

// Pass the API key at build time:
//   flutter run --dart-define=INDOORATLAS_API_KEY=<your key>
// or enter it in the text field in the app.
const _defaultApiKey = String.fromEnvironment('INDOORATLAS_API_KEY');

// The plugin rejects keys of any other length.
const _apiKeyLength = 36;

void main() {
  runApp(const MyApp());
}

class MyApp extends StatelessWidget {
  const MyApp({Key? key}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'IndoorAtlas Flutter example',
      theme: ThemeData(colorSchemeSeed: Colors.blue, useMaterial3: true),
      home: const ExamplePage(),
    );
  }
}

class ExamplePage extends StatefulWidget {
  const ExamplePage({Key? key}) : super(key: key);

  @override
  State<ExamplePage> createState() => _ExamplePageState();
}

class _ExamplePageState extends State<ExamplePage> {
  final _keyController = TextEditingController(text: _defaultApiKey);
  bool _running = false;
  IAStatus? _status;
  String _statusMessage = '';
  IALocation? _location;
  IAVenue? _venue;
  IAFloorplan? _floorplan;
  double? _heading;

  @override
  void dispose() {
    _keyController.dispose();
    super.dispose();
  }

  void _toggle() {
    final apiKey = _keyController.text.trim();
    if (!_running && apiKey.length != _apiKeyLength) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('Enter a valid $_apiKeyLength-character API key.'),
        ),
      );
      return;
    }
    setState(() {
      _running = !_running;
      if (_running) {
        IndoorAtlas.configure(IAConfiguration(apiKey: apiKey));
      } else {
        _status = null;
        _location = null;
        _venue = null;
        _floorplan = null;
        _heading = null;
      }
    });
  }

  @override
  Widget build(BuildContext context) {
    final loc = _location;
    final rows = <String>[
      'Status: ${_status?.name ?? '-'} $_statusMessage',
      'Trace ID: ${IndoorAtlas.traceId ?? '-'}',
      'Location: ${loc == null ? '-' : '${loc.latitude.toStringAsFixed(6)}, '
                '${loc.longitude.toStringAsFixed(6)}'}',
      'Accuracy: ${loc?.accuracy.toStringAsFixed(1) ?? '-'} m',
      'Floor: ${loc?.floor ?? '-'}',
      'Heading: ${_heading?.toStringAsFixed(1) ?? '-'}',
      'Venue: ${_venue?.name ?? '-'}',
      'Floorplan: ${_floorplan?.name ?? '-'}',
    ];
    return Scaffold(
      appBar: AppBar(title: const Text('IndoorAtlas Flutter example')),
      body: IndoorAtlasListener(
        name: 'com.indooratlas.flutter.example',
        // A new key on every toggle makes the listener go through subscribe() (which
        // starts positioning) instead of resubscribe() when it is enabled again.
        key: ValueKey(_running),
        enabled: _running,
        onStatus: (status, message) => setState(() {
          _status = status;
          _statusMessage = message;
        }),
        onLocation: (location) => setState(() => _location = location),
        onVenue: (enter, venue) =>
            setState(() => _venue = enter ? venue : null),
        onFloorplan: (enter, floorplan) =>
            setState(() => _floorplan = enter ? floorplan : null),
        onHeading: (heading) => setState(() => _heading = heading),
        child: ListView(
          padding: const EdgeInsets.all(16),
          children: [
            TextField(
              key: const Key('apiKeyField'),
              controller: _keyController,
              enabled: !_running,
              decoration: const InputDecoration(labelText: 'API key'),
            ),
            const SizedBox(height: 16),
            FilledButton(
              key: const Key('toggleButton'),
              onPressed: _toggle,
              child: Text(_running ? 'Stop positioning' : 'Start positioning'),
            ),
            const SizedBox(height: 16),
            for (final row in rows) Text(row, key: Key(row.split(':')[0])),
          ],
        ),
      ),
    );
  }
}

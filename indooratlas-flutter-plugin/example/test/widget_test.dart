import 'package:flutter/widgets.dart';
import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:indooratlas_example/main.dart';

void main() {
  const channel = MethodChannel('com.indooratlas.flutter');
  const apiKey = '00000000-0000-0000-0000-000000000000';

  final calls = <String>[];

  setUp(() {
    calls.clear();
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
        .setMockMethodCallHandler(channel, (MethodCall call) async {
          calls.add(call.method);
          return null;
        });
  });

  tearDown(() {
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
        .setMockMethodCallHandler(channel, null);
  });

  testWidgets('Shows the idle state', (WidgetTester tester) async {
    await tester.pumpWidget(const MyApp());

    expect(find.text('Start positioning'), findsOneWidget);
    expect(find.text('Status: - '), findsOneWidget);
  });

  testWidgets('Rejects an API key of the wrong length', (
    WidgetTester tester,
  ) async {
    await tester.pumpWidget(const MyApp());
    await tester.enterText(find.byKey(const Key('apiKeyField')), 'too-short');
    await tester.tap(find.byKey(const Key('toggleButton')));
    await tester.pump();

    expect(calls, isEmpty);
    expect(find.text('Start positioning'), findsOneWidget);
    expect(find.text('Enter a valid 36-character API key.'), findsOneWidget);
  });

  testWidgets('Stop and start again restarts positioning', (
    WidgetTester tester,
  ) async {
    await tester.pumpWidget(const MyApp());
    await tester.enterText(find.byKey(const Key('apiKeyField')), apiKey);

    Future<void> toggle() async {
      await tester.tap(find.byKey(const Key('toggleButton')));
      await tester.pump();
    }

    await toggle(); // start
    expect(calls, contains('startPositioning'));
    expect(
      calls.lastIndexOf('startPositioning'),
      greaterThan(calls.lastIndexOf('stopPositioning')),
    );

    await toggle(); // stop
    expect(calls.last, isNot('startPositioning'));
    expect(calls, contains('stopPositioning'));

    calls.clear();
    await toggle(); // start again with the same key
    expect(calls, contains('startPositioning'));
    expect(
      calls.lastIndexOf('startPositioning'),
      greaterThan(calls.lastIndexOf('stopPositioning')),
    );
  });
}

import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:indooratlas/indooratlas.dart';

void main() {
  const MethodChannel channel = MethodChannel('com.indooratlas.flutter');

  TestWidgetsFlutterBinding.ensureInitialized();

  final calls = <MethodCall>[];

  setUp(() {
    calls.clear();
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
        .setMockMethodCallHandler(channel, (MethodCall methodCall) async {
      calls.add(methodCall);
      return null;
    });
  });

  tearDown(() {
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
        .setMockMethodCallHandler(channel, null);
  });

  test('configure initializes the native side with the api key', () {
    const apiKey = '00000000-0000-0000-0000-000000000000';
    IndoorAtlas.configure(const IAConfiguration(apiKey: apiKey));

    expect(calls.first.method, 'initialize');
    expect(calls.first.arguments, ['0.0.1', apiKey, '']);
    expect(IndoorAtlas.configuration.apiKey, apiKey);
  });
}

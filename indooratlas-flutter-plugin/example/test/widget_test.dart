import 'package:flutter_test/flutter_test.dart';

import 'package:indooratlas_example/main.dart';

void main() {
  testWidgets('Shows the idle state', (WidgetTester tester) async {
    await tester.pumpWidget(const MyApp());

    expect(find.text('Start positioning'), findsOneWidget);
    expect(find.text('Status: - '), findsOneWidget);
  });
}

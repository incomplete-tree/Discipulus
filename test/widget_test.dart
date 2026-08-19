import 'package:discipulus/core/desktop_integration.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  group('DesktopLaunchIntent', () {
    test('parses supported routes from command-line arguments', () {
      final intent = DesktopLaunchIntent.fromArgs([
        '--verbose',
        '--route=calendar',
      ]);

      expect(intent?.route, 'calendar');
      expect(intent?.destinationLabel, 'Kalender');
    });

    test('parses the KDE URL handler and the separated route form', () {
      expect(
        DesktopLaunchIntent.fromArgs(['discipulus://calendar'])?.route,
        'calendar',
      );
      expect(
        DesktopLaunchIntent.fromArgs(['--route', 'messages'])?.route,
        'messages',
      );
    });

    test('accepts route names case-insensitively', () {
      expect(DesktopLaunchIntent.fromRoute('GRADES')?.route, 'grades');
      expect(DesktopLaunchIntent.fromRoute(' messages ')?.route, 'messages');
    });

    test('ignores unsupported or missing routes', () {
      expect(DesktopLaunchIntent.fromArgs(['--route=unknown']), isNull);
      expect(
          DesktopLaunchIntent.fromArgs(['file:///tmp/calendar.ics']), isNull);
      expect(DesktopLaunchIntent.fromRoute(null), isNull);
      expect(DesktopLaunchIntent.fromRoute(''), isNull);
    });
  });
}

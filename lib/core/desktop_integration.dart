import 'dart:io';

import 'package:flutter/services.dart';

/// A route requested by a desktop launcher, task-manager action, or command
/// line invocation.
class DesktopLaunchIntent {
  const DesktopLaunchIntent._(this.route);

  final String route;

  static DesktopLaunchIntent? fromArgs(Iterable<String> args) {
    final arguments = args.toList();
    for (var index = 0; index < arguments.length; index++) {
      final argument = arguments[index].trim();
      if (argument.startsWith('--route=')) {
        final intent = fromRoute(argument.substring('--route='.length));
        if (intent != null) return intent;
      } else if (argument == '--route' && index + 1 < arguments.length) {
        final intent = fromRoute(arguments[++index]);
        if (intent != null) return intent;
      }

      final uri = Uri.tryParse(argument);
      if (uri?.scheme.toLowerCase() == 'discipulus') {
        final route = uri!.host.isNotEmpty
            ? uri.host
            : uri.pathSegments.isEmpty
                ? null
                : uri.pathSegments.first;
        final intent = fromRoute(route);
        if (intent != null) return intent;
      }
    }
    return null;
  }

  static DesktopLaunchIntent? fromRoute(String? value) {
    final route = value?.trim().toLowerCase();
    if (route == null ||
        !const {'calendar', 'grades', 'messages'}.contains(route)) {
      return null;
    }
    return DesktopLaunchIntent._(route);
  }

  String get destinationLabel => switch (route) {
        'calendar' => 'Kalender',
        'grades' => 'Recente cijfers',
        'messages' => 'Berichten',
        _ => throw StateError('Unsupported desktop route: $route'),
      };
}

/// Linux-specific communication with the native GTK host.
///
/// The channel is intentionally kept in the application instead of a plugin:
/// the native side only needs to deliver launcher actions to the already
/// running Flutter view, and this keeps the integration dependency-free.
class DesktopIntegration {
  static const MethodChannel _channel =
      MethodChannel('dev.harrydekat.discipulus/desktop');

  static void bind(Future<void> Function(String route) onRoute) {
    if (!Platform.isLinux) return;

    _channel.setMethodCallHandler((call) async {
      if (call.method != 'openRoute') return null;

      final intent = DesktopLaunchIntent.fromRoute(call.arguments?.toString());
      if (intent != null) await onRoute(intent.route);
      return null;
    });
  }
}

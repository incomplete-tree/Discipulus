# Discipulus handoff status

This is a snapshot of the worktree at handoff. No commit or push was made.

## Present in the source tree

- Linux desktop packaging and KDE launcher actions.
- Linux desktop route handling for Calendar, Grades, and Messages.
- Android launcher widgets with small, medium, and rectangular layouts.
- Wear OS module/build configuration and release ProGuard rules.
- CI and GitHub Release workflow definitions.
- Dart tests for desktop route parsing.

## Validated in the completion pass

- `flutter pub get` completed with the public Fleather compatibility pin.
- `dart format --output=none --set-exit-if-changed lib test` passed.
- `flutter analyze --no-fatal-infos --no-fatal-warnings` passed with the
  existing non-fatal diagnostics only.
- `flutter test` passed, including the desktop route parsing tests.
- `flutter build linux --release` produced a working x86_64 bundle.
- The Linux installer was exercised in isolated XDG directories. It installed
  the executable, launcher actions, AppStream metadata, and icon, and the
  packaged tarball passed archive validation.
- The rebuilt bundle was installed into the active KDE user session, launched
  successfully through the installed binary, and remained running. KDE's
  desktop entry, icon, AppStream metadata, URL handler, and quick actions were
  verified in place. Linux notification initialization and the non-iOS Apple
  Watch plugin call were guarded so first launch reaches the login screen.
- The KDE Wayland login freeze was reproduced as repeated EGL context failures
  from Flutter's Impeller renderer. The Linux host now disables Impeller for
  this desktop build; the installed repaired process remained alive through a
  stability check with no further `eglMakeCurrent failed` errors.
- A Plasma 6 widget was added, installed, placed on the active desktop, and
  reloaded without widget-specific QML errors. It provides direct Calendar,
  Grades, Messages, and app launch actions through the existing URL handler.
- Debian `adb` 1:34.0.5-12 was installed on the host. Fresh phone and Wear OS
  release APKs were built, v2-verified, and copied to the handoff artifacts
  directory; no Android or Wear device was connected for live installation.
- `flutter build apk --release` produced the phone APK and
  `:wear:assembleRelease` produced the Wear OS APK. Both passed ZIP and package
  ID validation (`dev.harrydekat.discipulus`). Local artifacts use the debug
  key because production signing credentials are not present here.
- Both Android production variants fail closed when
  `DISCIPULUS_RELEASE_BUILD=true` is set without signing credentials.
- The CI and release workflows parse as YAML, use the public repositories only,
  configure both Android SDK and Flutter paths, and require production signing
  secrets for a release.

## Environment-limited checks

- No Android emulator, Wear OS emulator, physical device, or watch was
  available for live widget rendering, tap navigation, or Wear Data Layer
  testing. The native widget code is covered by the Android release compile and
  malformed/stale event guards; live visual behavior still needs device QA.
- A production-signed APK was not generated locally. The release workflow
  requires `CM_KEYSTORE_BASE64`, `CM_KEYSTORE_PASSWORD`, `CM_KEY_ALIAS`, and
  `CM_KEY_PASSWORD` before it can publish.
- The local Debian image provided JDK 21 rather than JDK 17, and the local
  Android builds passed with JDK 21. CI explicitly pins Temurin JDK 17.
- The handoff directory has no usable Git metadata, so no commit or push was
  made and a repository diff cannot be generated here.

Generated directories, local dependency caches, and machine-specific toolchains
are intentionally excluded from the source portion of the ZIP.

# Discipulus handoff status

This is the validated handoff status for the stacked feature branches. No
remote push was made.

## Present in the source tree

- Linux desktop packaging and KDE launcher actions.
- Linux desktop route handling for Calendar, Grades, and Messages.
- Android launcher widgets with small, medium, and rectangular layouts.
- Android calendar widgets refresh from the calendar even when notification
  settings are disabled, and show the next lesson when none is in progress.
- Apple WidgetKit calendar, grades, and messages entries.
- KDE Plasma Calendar, Grades, and Messages applet configuration.
- Wear OS Calendar, Grades, and Messages Tiles and complication providers.
- The Wear Agenda complication supports `SHORT_TEXT`, `LONG_TEXT`, and
  `SMALL_IMAGE` for Samsung square/small-box slots as well as edge slots.
- Wear OS phone-to-watch synchronization for schedule, grades, and messages.
- Wear OS homework entries are marked `HW`; cancelled lessons are synced and
  controlled by the `Uitgevallen lessen tonen` setting. Cancelled lessons do
  not schedule haptic reminders.
- Wear OS module/build configuration and release ProGuard rules.
- CI and GitHub Release workflow definitions.
- Dart tests for desktop route parsing.

## Validated in the completion pass

- `dart format --output=none --set-exit-if-changed lib test` passed.
- `flutter analyze --no-fatal-infos --no-fatal-warnings` passed with the
  existing 92 non-fatal diagnostics only.
- `flutter test` passed in the earlier completion pass, including the desktop
  route parsing tests.
- `flutter build linux --release` produced the current x86_64 bundle.
- `flutter build apk --release` produced the current phone APK with the three
  Android widgets, the expanded Wear sync payload, and homework/cancellation
  support.
- `:wear:assembleRelease --no-daemon` compiled all three Tiles and all three
  complication services.
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
  reloaded without widget-specific QML errors. The current package is loaded
  by three active applets configured as Calendar, Grades, and Messages; the
  launcher actions remain separate from these desktop widgets.
- Debian `adb` 1:34.0.5-12 was installed on the host. Fresh phone and Wear OS
  release APKs were built, v2-verified, and copied to `artifacts/`.
- Both APKs passed package validation for `dev.harrydekat.discipulus`, target
  SDK 36, and APK Signature Scheme v2. Local artifacts use the debug key
  because production signing credentials are not present here.
- Current local artifact SHA-256 values are:
  - Android phone: `fb665c4001f247d50f95e72ac52949930d159ec56169b60a8f94b00f1cf4b4a6`
  - Wear OS: `cdeea2e871e440bda900fb671ad2dfa5203d10795fb06cf17d098678bc1e14da`
- Both Android production variants fail closed when
  `DISCIPULUS_RELEASE_BUILD=true` is set without signing credentials.
- The CI and release workflows parse as YAML, use the public repositories only,
  configure both Android SDK and Flutter paths, and require production signing
  secrets for a release.
- The README rules were rechecked against the source tree: no local Maven
  repository entries remain, `android/local.properties` is ignored, generated
  artifacts are ignored, public dependency sources are used, and production
  signing is required by CI.

## Environment-limited checks

- The Galaxy Watch was reachable over ADB for package inspection, but its
  network ADB endpoint dropped before the rebuilt APK could be installed.
  Live Tile-carousel, complication rendering, and Data Layer round-trip QA
  still require reinstalling the new artifact on the watch.
- A final unprivileged `flutter test` rerun was blocked because the sandbox
  could not open the test harness's temporary loopback socket; the source test
  suite had passed in the earlier completion pass.
- A production-signed APK was not generated locally. The release workflow
  requires `CM_KEYSTORE_BASE64`, `CM_KEYSTORE_PASSWORD`, `CM_KEY_ALIAS`, and
  `CM_KEY_PASSWORD` before it can publish.
- The local Debian image provided JDK 21 rather than JDK 17, and the local
  Android builds passed with JDK 21. CI explicitly pins Temurin JDK 17.
- The current branch is `feature/wear-tiles`, stacked on
  `feature/widgets-kde`, `feature/widgets-apple`, `feature/widgets-android`,
  and `handoff/fix-wear-sync`. After refreshing
  `https://github.com/DiscipulusApp/Discipulus`, `upstream/main` is
  `bed89034a2de2cb92f6ff41467583349d8d7ab9d`; this stack is 0 commits behind
  and 8 commits ahead, with 94 intentional changed files. The original
  no-ancestor local refs are preserved as `backup/*`; no branch was pushed.

The active watch used in the earlier sync handoff is not currently reachable:
ADB reports no connected device and reconnecting its previous network endpoint
is refused. Therefore the final tile carousel, complication rendering, and
Data Layer round-trip still need live Wear OS device QA. Generated directories,
local dependency caches, and machine-specific toolchains are intentionally
excluded from the source portion of the ZIP.

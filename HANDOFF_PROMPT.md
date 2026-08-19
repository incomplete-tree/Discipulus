# Discipulus completion handoff

You are taking over a partially implemented cross-platform build of:
`https://github.com/DiscipulusApp/Discipulus`.

The user wants a high-quality, PR-ready implementation that provides:

- a Linux x86_64 release bundle with KDE Plasma integration;
- an Android phone APK;
- a Wear OS APK;
- Android home-screen widgets; and
- CI/CD that builds and publishes all release artifacts to GitHub Releases.

Work from the included source tree. Preserve useful existing changes and begin
with `git status --short` and a careful diff review. Do not discard changes or
rewrite unrelated application behavior. The current source has uncommitted
work; the package also contains `BUILD_STATUS.md`, which records what was
actually validated before handoff.

## First make the checkout reproducible

Use a network-enabled Linux machine with current normal toolchains. Prefer
Flutter stable, pinned consistently in CI and local documentation (the prior
validation used Flutter 3.47.0 stable). Install Java 17, Android SDK platform
36, build-tools 36.0.0, NDK 27.0.12077973, and CMake/Ninja. For Ubuntu/Debian,
install at least:

```bash
sudo apt-get update
sudo apt-get install -y clang cmake ninja-build pkg-config libgtk-3-dev \
  libepoxy-dev libsecret-1-dev libx11-dev libxcomposite-dev \
  libxdamage-dev libxfixes-dev libxrandr-dev
```

Resolve dependencies from the public repositories with `flutter pub get` and
Gradle using `google()`, `mavenCentral()`, and the Gradle plugin portal.

The current checkout contains temporary build-machine repository entries in
`android/settings.gradle.kts` and `android/build.gradle.kts` pointing at
`/workspace/toolchains/maven-repo`. Remove those local-only entries before
committing; they must not be required on a normal developer machine or GitHub
Actions runner. Do not commit `android/local.properties`, generated build
directories, or downloaded caches. Ensure `android/local.properties` used by
CI contains both values, for example:

```properties
sdk.dir=/path/to/Android/sdk
flutter.sdk=/path/to/flutter
```

Use an appropriate local SDK path or `$ANDROID_SDK_ROOT` on the runner. Keep
credentials out of the repository.

## Required verification loop

Run and fix all failures, rather than weakening checks:

```bash
flutter pub get
dart format --set-exit-if-changed lib test
flutter analyze --no-fatal-infos --no-fatal-warnings
flutter test
flutter build linux --release
flutter build apk --release
cd android
./gradlew :wear:assembleRelease --no-daemon
cd ..
```

If the Fleather compatibility ref in `pubspec.yaml` is still needed for the
installed Flutter version, verify that it resolves from its public Git source
and document why. Do not replace a real dependency with a machine-local copy.

Validate outputs with `file`, `unzip -t`, and Android tooling (`aapt dump
badging` or `apkanalyzer`). Confirm the phone APK and Wear APK have the
intended package/application IDs and that release builds are installable.

## KDE Plasma / Linux acceptance criteria

Review the Linux implementation as a KDE user would:

1. Install the bundle with `linux/packaging/install.sh` into a temporary user
   data directory and verify the executable, icon, AppStream metadata, and
   desktop file are relocatable.
2. Run `update-desktop-database`/`kbuildsycoca6` when available and verify the
   launcher appears in Plasma.
3. Verify the desktop actions for Calendar, Grades, and Messages launch the
   corresponding in-app route, including when launched from the task manager
   context menu.
4. Verify `discipulus://calendar` opens the calendar view and that a normal
   `%U` launch still works. Avoid breaking single-instance/window behavior.
5. Keep the desktop integration optional and isolated from mobile builds; use
   platform guards or the existing channel boundary rather than importing GTK
   code into shared Dart code.

Consider adding focused tests for argument/URI parsing and document any KDE
version-specific behavior. Do not claim deeper Plasma features such as a
background service, tray icon, global shortcuts, or DBus notifications unless
they are implemented, tested, and have a clear lifecycle/uninstall story.

## Android widget acceptance criteria

Build and install the phone APK on an emulator/device. Add the widget from the
launcher and verify small, medium, and rectangular layouts in light and dark
themes. Verify:

- updates do not crash on empty, malformed, or stale event data;
- dates/times and the next-event state are readable at launcher sizes;
- tapping the widget opens the intended Discipulus calendar route;
- the receiver is declared correctly and does not unnecessarily export an
  attack surface; and
- updates are scheduled through the existing Flutter/background task flow with
  sensible failure handling.

Keep Android resource IDs, exported flags, PendingIntent mutability, and
compatibility behavior correct for the target SDK. Add unit or instrumentation
coverage where practical.

## Wear OS acceptance criteria

Build the Wear module on a clean machine with network access. Inspect the
manifest, Compose APIs, min/target SDKs, signing configuration, and ProGuard
rules. Install to an emulator/watch with `adb` and verify startup, navigation,
and the behavior represented by the current implementation. Do not silently
ship a debug-signed artifact as the official release artifact.

For public releases, configure stable signing secrets. The current workflow
supports these environment variables:

```text
CM_KEYSTORE_BASE64
CM_KEYSTORE_PASSWORD
CM_KEY_ALIAS
CM_KEY_PASSWORD
```

Decode the base64 keystore in the workflow into a temporary runner path and
use the same signing identity for phone and Wear releases. If the workflow
keeps an ephemeral fallback for smoke builds, clearly label it and never use it
for a production release without user approval.

## CI/CD acceptance criteria

Review `.github/workflows/ci.yml` and `release.yml` on a clean GitHub runner.
The PR workflow must run formatting, analysis, tests, and at least a meaningful
Android/Wear compile check. The release workflow must:

- trigger on version tags such as `v1.2.3` and support manual dispatch;
- install all Linux and Android prerequisites;
- create `android/local.properties` with both `sdk.dir` and `flutter.sdk`;
- build Linux, phone Android, and Wear OS artifacts;
- package the Linux bundle with the launcher, AppStream metadata, icon, and
  installer;
- generate SHA-256 checksums;
- upload artifacts and publish them to the matching GitHub Release; and
- fail loudly if an expected output is missing.

Use least-privilege permissions and pin third-party actions to maintained major
versions (or commit SHAs if the project policy requires it). Check shell
quoting, version parsing, release naming, and artifact paths. Run a YAML
lint/parse check and, where possible, exercise the workflow with a local or
CI dry run.

## PR-quality finish

Update the README with build/install instructions for Linux, Android, and Wear
OS, KDE-specific launcher actions, widget setup, signing, and release assets.
Add tests for new pure logic and keep platform code small and documented. Run
`git diff --check`, formatting, analysis, tests, and all relevant builds. Review
the final diff for generated files, hard-coded workstation paths, secrets,
unrelated refactors, and missing licenses. Make a focused commit or draft PR,
but do not push or publish anything unless the user explicitly requests it.

The deliverable is complete only when the clean networked build succeeds and
the release workflow can produce all three binaries plus checksums.

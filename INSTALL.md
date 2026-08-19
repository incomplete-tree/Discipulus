# Discipulus snapshot installation

## Linux / KDE Plasma

Download `Discipulus-linux-x64.tar.gz` from the GitHub Release and extract it:

```bash
tar -xzf Discipulus-linux-x64.tar.gz
./install.sh
```

Run the installer as the target user. It installs the relocatable bundle below
`${XDG_DATA_HOME:-~/.local/share}/discipulus`, a launcher and AppStream file,
an icon, and a `discipulus` link below `${XDG_BIN_HOME:-~/.local/bin}`. KDE
Plasma quick actions open Calendar, Grades, and Messages. The installer
refreshes the desktop database and KDE service cache when those tools are
available.

The `discipulus://calendar` URL handler opens the calendar view. A normal
launcher invocation with `%U` remains supported, including file or unknown URI
arguments that should simply open the app.

### Plasma desktop widget

On KDE Plasma 6, install the included widget with:

```bash
kpackagetool6 --type Plasma/Applet --install linux/plasma-widget
```

Then right-click the desktop, choose **Enter Edit Mode** → **Add Widgets**, and
search for **Discipulus**. The widget provides one-click access to the calendar,
grades, messages, and the app.

## Android phone and widgets

Install `Discipulus-android.apk` on an Android phone. Open the launcher widget
picker, choose Discipulus, and resize it to check the small, medium, and
rectangular layouts. The widget updates from the app's existing 30-minute
background refresh and opens the calendar when tapped.

## Wear OS

Install `Discipulus-wearos.apk` on a Wear OS emulator or watch with the matching
phone app. The app provides schedule, grades, settings, reminders, and a
complication data source. Public builds must use the stable keystore configured
in GitHub Actions; local debug signing is only for development installs.

## Building locally

Use Flutter **3.47.0 stable**, Java 17, Android SDK platform 36, build-tools
36.0.0, NDK 27.0.12077973, CMake, Ninja, and the Linux GTK development
packages. Resolve dependencies and run the checks/builds from the repository
root:

```bash
flutter pub get
dart format --set-exit-if-changed lib test
flutter analyze --no-fatal-infos --no-fatal-warnings
flutter test
flutter build linux --release
flutter build apk --release
cd android
./gradlew :wear:assembleRelease --no-daemon
```

For direct Gradle invocation, create the ignored `android/local.properties`
with both paths:

```properties
sdk.dir=/path/to/Android/sdk
flutter.sdk=/path/to/flutter
```

For Flutter's Android commands, also set `ANDROID_SDK_ROOT` or configure the
SDK once with `flutter config --android-sdk /path/to/Android/sdk`.

Never commit that file, generated `build/` directories, downloaded caches, or
keystores.

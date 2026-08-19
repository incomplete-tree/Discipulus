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
search for **Discipulus**. Add three copies and configure them individually as
**Calendar**, **Grades**, and **Messages** from **Configure Discipulus**.

## Android phone and widgets

Install `Discipulus-android.apk` on an Android phone. Open the launcher widget
picker, and add the **Calendar**, **Grades**, and **Messages** widgets. They
update from the app's existing 30-minute background refresh and open their
matching app view when tapped.

For a local install with ADB:

```bash
adb install -r Discipulus-android.apk
```

## Wear OS

Install `Discipulus-wearos.apk` on a Wear OS emulator or watch with the matching
phone app. Install the phone APK first, then the Wear APK. The app provides
schedule, grades, settings, reminders, three Tiles (Agenda, Cijfers, Berichten),
and three complication providers. Public builds must use the stable keystore
configured in GitHub Actions; local debug signing is only for development
installs.

For a connected Wear OS device:

```bash
adb install -r Discipulus-android.apk
adb -s <wear-device-serial> install -r Discipulus-wearos.apk
```

Open the watch's Tiles carousel to add the three Discipulus Tiles. In the
watch-face editor, choose Discipulus as a provider for the supported complication
slot types.

To see homework, open the watch app and choose **Rooster**. Homework entries are
marked **HW** and can be tapped to mark them complete. To include cancelled
lessons, open **Instellingen** and enable **Uitgevallen lessen tonen**; they are
then shown with a cancelled label but do not trigger reminders.

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

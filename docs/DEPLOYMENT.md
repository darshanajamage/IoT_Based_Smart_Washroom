# Deployment - how to build, sign and install the APK

This document explains how to build the app, how to install it on a phone and how
to prepare a release build for the college submission.

---

## 1. Build the debug APK (for testing)

### Android Studio

1. **Build > Build Bundle(s) / APK(s) > Build APK(s)**
2. The APK is written to:

   ```
   app/build/outputs/apk/debug/app-debug.apk
   ```

### Command line

```bash
# Windows
gradlew.bat :app:assembleDebug

# macOS / Linux
./gradlew :app:assembleDebug
```

The debug APK is signed automatically with the **debug key** that Android Studio
generates on the first build, so it can be installed on any phone right away.

---

## 2. Install the APK on a phone

### Over USB with adb

```bash
# Windows
adb install -r app\build\outputs\apk\debug\app-debug.apk

# macOS / Linux
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

* `-r` replaces an already installed version and keeps the app data.
* `adb devices` shows the connected phones.
* USB debugging has to be switched on in
  **Settings > About phone > Build number** (tap 7 times) and then in
  **Settings > Developer options > USB debugging**.

### By copying the file

1. Copy `app-debug.apk` to the phone (USB cable, Google Drive, e-mail).
2. Tap the file on the phone.
3. Android asks for the permission "Install unknown apps" - allow it for the app
   you opened the file with.

---

## 3. Build a release APK

```bash
gradlew.bat :app:assembleRelease
```

Output:

```
app/build/outputs/apk/release/app-release-unsigned.apk
```

The file is **not signed**, because the project does not contain a release
signing key yet. It cannot be installed until it is signed.

---

## 4. Create a keystore (only once)

```bash
keytool -genkeypair -v \
  -keystore smartbathroom.jks \
  -alias smartbathroom \
  -keyalg RSA -keysize 2048 -validity 10000
```

Answer the questions (a password, a name, ...). **Keep the keystore file and the
passwords safe and private.** Never commit the keystore to the repository and
never share the passwords.

---

## 5. Sign the release APK

### With Android Studio

1. **File > Project Structure > Signing Configs**
2. **+** -> **Generate Upload Key** (or use your own keystore) and fill in the
   details.
3. In `app/build.gradle.kts` add the signing config to the `release` build type:

   ```kotlin
   buildTypes {
       release {
           isMinifyEnabled = false
           proguardFiles(
               getDefaultProguardFile("proguard-android-optimize.txt"),
               "proguard-rules.pro"
           )
           signingConfig = signingConfigs.getByName("upload")   // <- new
       }
   }
   ```

4. Build again with `gradlew.bat :app:assembleRelease`.

### With the command line only (apksigner)

```bash
# 1. zipalign (makes the installation faster)
zipalign -v 4 app-release-unsigned.apk app-release-aligned.apk

# 2. sign
apksigner sign --ks smartbathroom.jks --ks-key-alias smartbathroom \
  --out app-release-signed.apk app-release-aligned.apk

# 3. check the signature
apksigner verify --verbose app-release-signed.apk
```

The tools are in the Android SDK:

```
%LOCALAPPDATA%\Android\Sdk\build-tools\36.0.0\
```

---

## 6. Where to put the keystore (important)

| File | Where | Should it be in the repository? |
|---|---|---|
| `smartbathroom.jks` | your private folder, or `C:\Users\<you>\.android\` | **No** |
| `keystore.properties` (with the passwords) | your private folder | **No** |
| `google-services.json` (later, for Firebase) | `app/` | Yes, but it must not contain any secret key |

Add the private files to `.gitignore`.

---

## 7. Versioning

The version is set in `app/build.gradle.kts`:

```kotlin
defaultConfig {
    versionCode = 1      // must be increased for every upload
    versionName = "1.0"  // the name shown to the user
}
```

For the next release, for example `versionCode = 2` and `versionName = "1.1"`.

---

## 8. Handing the APK to the college

| Option | How |
|---|---|
| USB stick | Copy the APK, the student installs it on their own phone |
| Google Drive / e-mail | Upload the signed release APK and share the link |
| Google Play (internal test track) | Requires a developer account (25 USD once) and the signed release APK |

Recommended for a college demo: the **debug APK** for the project members and a
**signed release APK** for the submission.

---

## 9. Before you submit - checklist

- [ ] The app starts without crashing on Android 7 (API 24) and on the newest
      Android version
- [ ] Demo Mode creates both alert types
- [ ] The contacts survive an app restart
- [ ] The alert history survives an app restart
- [ ] The build of the APK was made from the same commit as the submitted source code
- [ ] The keystore and its passwords are **not** part of the submission
- [ ] The README and the `docs/` folder are up to date
- [ ] The app version was increased

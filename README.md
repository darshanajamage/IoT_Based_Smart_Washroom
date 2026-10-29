# Smart Bathroom Safety

**Fall Detection and Emergency Response System**
College project - Android app (Java + XML layouts).

The app is the phone side of a small safety device that is placed in a bathroom.
The device watches for movement and has a physical emergency button. When
something happens, the phone shows a clear warning screen, helps the user call a
family member and shows the nearby hospitals.

---

## 1. What works today

| Area | Status | Notes |
|---|---|---|
| Home dashboard | Done | App name, device connection status, current status, latest alert, 4 navigation buttons |
| Alert warning screen | Done | Separate screen for **NO MOTION DETECTED** and **EMERGENCY BUTTON PRESSED** |
| Family contacts | Done | Add / edit / delete, stored locally, "Call" button per contact |
| Nearby hospitals | Done | Opens Google Maps with a hospital search, handles "Maps not installed" |
| Alert history | Done | Local list with type, date, time and a **DEMO** badge, can be cleared |
| Device status | Done | `ONLINE`, `OFFLINE`, `NO MOTION ALERT`, `EMERGENCY ALERT` (kept separate) |
| Demo mode | Done | Simulates both alert types and the online/offline state, no hardware needed |
| Notifications | Done | Local notification on the phone when a demo alert is raised |
| Firebase | **PENDING** | The code is prepared, the cloud connection is **not** active yet |

### Honest statement about the hardware

**No real hardware is connected to this app yet.** The alerts you can create today
are simulated by the **Demo Mode** screen and are clearly marked with a `DEMO`
badge everywhere they appear. The app does **not** receive alerts from a real
device, and it does **not** talk to a Firebase project yet.

---

## 2. Requirements

* Android Studio (the project was created with the version that ships AGP 9.0.1)
* JDK 17 (bundled with Android Studio)
* Android SDK with platform **API 36** and build tools **36.0.0**
* A phone or emulator running **Android 7.0 (API 24)** or newer

The Gradle files were **not changed**. The project uses the versions that were
already installed:

* Gradle 9.1.0 (Gradle wrapper)
* Android Gradle Plugin 9.0.1
* `compileSdk` / `targetSdk` 36, `minSdk` 24, Java 11 source compatibility
* Dependencies: appcompat, material, activity, constraintlayout

---

## 3. How to open the project

1. Start **Android Studio**.
2. Choose **File > Open** and select the project folder (the folder that contains
   `settings.gradle.kts`, `app/`, `gradlew.bat` and `gradle/`).
3. Wait until the **Gradle Sync** finishes (the bottom bar shows a progress bar).
   The first sync downloads the Android SDK components, so it can take a few
   minutes.
4. If Android Studio asks to install missing SDK components, accept them.

> The Java package name of the project is still `com.example.smartwashroom`
> (the name of the original Android Studio project). The **visible** app name is
> "Smart Bathroom Safety". If you want to change the package name as well, use
> **Refactor > Rename** in Android Studio - never rename the folders by hand.

---

## 4. How to run the app

### From Android Studio

1. Connect a phone with USB debugging enabled, or start an emulator
   (**Device Manager > Create Device**).
2. Select the **app** run configuration and your device.
3. Press the green **Run** button (or `Shift + F10`).
4. The app opens on the **Home dashboard**.

### From the command line

```bash
# Windows
gradlew.bat :app:installDebug

# macOS / Linux
./gradlew :app:installDebug
```

---

## 5. How to build the app

```bash
# Windows
gradlew.bat :app:assembleDebug        # -> app\build\outputs\apk\debug\app-debug.apk
gradlew.bat :app:assembleRelease      # -> app\build\outputs\apk\release\app-release-unsigned.apk

# macOS / Linux
./gradlew :app:assembleDebug
./gradlew :app:assembleRelease
```

The debug build is signed with the automatic debug key, so it can be installed
directly. The release build is **not signed** - see `docs/DEPLOYMENT.md` for the
signing and distribution steps.

Other useful commands:

```bash
gradlew.bat :app:lintDebug      # static checks
gradlew.bat :app:testDebugUnitTest
gradlew.bat clean
```

---

## 6. How to test the app (5 minutes, no hardware)

1. Open the app -> tap **Demo Mode**.
2. Tap **Simulate No Motion Alert**.
   * an amber warning screen opens,
   * the notification appears in the notification shade,
   * the alert is stored in **Alert History** with a `DEMO` badge,
   * the dashboard shows the status `NO MOTION ALERT`.
3. Tap **Call a Family Member** -> pick a contact -> the dialler opens with the
   number (the app never places a call by itself).
4. Tap **I Have Handled This** -> the dashboard is back to `ONLINE`.
5. Go back to **Demo Mode** -> tap **Simulate Emergency Button Press**.
   * the screen is red this time,
   * the dashboard shows `EMERGENCY ALERT`.
6. Use **Simulate Device Offline** in Demo Mode to test the `OFFLINE` state.
   *Important:* `OFFLINE` and `NO MOTION ALERT` are two different states.
   Offline means "the phone cannot reach the device", a no-motion alert means
   "the device is talking to us and reports that nobody moved".
7. Add a contact under **Emergency Contacts** -> **Add Contact**. Try saving an
   empty form or a wrong number, the form refuses to save and shows the reason.
8. Open **Nearby Hospitals** -> tap the Google Maps button. If Google Maps is not
   installed on the device, the app explains it and offers the browser version.

---

## 7. Where the data is stored

Everything is stored **on the phone only**, in `SharedPreferences`:

| File | Content |
|---|---|
| `family_contacts` | Name and phone number of the family members (JSON) |
| `alert_history` | All stored alerts with type, time and the demo flag (JSON) |
| `device_status` | Connection status and the active alert |

No personal data is sent anywhere. Uninstalling the app removes the data.

---

## 8. Project structure

```
app/src/main/java/com/example/smartwashroom/
├── SafetyApplication.java            creates the notification channel
├── MainActivity.java                 HOME DASHBOARD
├── model/
│   ├── AlertType.java                NO MOTION / EMERGENCY BUTTON
│   ├── DeviceStatus.java             ONLINE / OFFLINE / NO MOTION ALERT / EMERGENCY ALERT
│   ├── AlertRecord.java              one entry of the alert history
│   └── FamilyContact.java            one family member
├── data/
│   ├── FirebaseRepository.java       <-- the only class that will talk to Firebase (PENDING)
│   ├── ContactRepository.java        local contact storage
│   ├── local/                        SharedPreferences stores
│   └── source/
│       ├── AlertDataSource.java      interface for the alert data
│       ├── LocalDemoAlertDataSource.java   the implementation used today
│       ├── RepositoryCallback.java
│       └── DeviceStatusCallback.java
├── ui/
│   ├── BaseActivity.java             shared app bar + insets + permission handling
│   ├── AlertActivity.java            ALERT WARNING SCREEN
│   ├── ContactsActivity.java         FAMILY CONTACTS
│   ├── ContactEditActivity.java      ADD / EDIT CONTACT
│   ├── AlertHistoryActivity.java     ALERT HISTORY
│   ├── DemoModeActivity.java         DEMO MODE
│   ├── NearbyHospitalsActivity.java  NEARBY HOSPITALS
│   └── NotificationHelper.java       local safety notification
└── util/
    ├── PhoneNumberValidator.java     phone number check
    ├── DialerHelper.java             ACTION_DIAL
    └── DateTimeUtils.java            date / time formatting

app/src/main/res/layout/   activity_*.xml and item_*.xml (XML layouts only)
app/src/main/res/drawable/ the icons and shapes of the app
app/src/main/res/values/   strings, colours, themes
docs/                       the documentation of this project
```

---

## 9. Documentation

| File | Content |
|---|---|
| [`docs/FIREBASE_INTEGRATION.md`](docs/FIREBASE_INTEGRATION.md) | What is needed to switch the Firebase Realtime Database on |
| [`docs/HARDWARE_TEAM_CHECKLIST.md`](docs/HARDWARE_TEAM_CHECKLIST.md) | Questions for the hardware teammate |
| [`docs/DEPLOYMENT.md`](docs/DEPLOYMENT.md) | How to build, sign and install the APK |
| [`docs/CALLING_AND_NOTIFICATIONS.md`](docs/CALLING_AND_NOTIFICATIONS.md) | How calling and notifications work |

---

## 10. Troubleshooting

| Problem | Solution |
|---|---|
| "SDK location not found" | Open `local.properties` and set `sdk.dir` to your Android SDK folder |
| Gradle sync fails | Check your internet connection and that the Gradle version matches the wrapper |
| Notification does not appear | Android 13+ asks for the notification permission at the first start; also check the battery saver |
| "No phone dialler app was found" | The device has no dialler (for example a tablet). The app shows this message instead of crashing |
| Google Maps does not open | The app detects a missing Google Maps and offers the browser version |
| Alerts are gone after reinstalling the app | The history is stored on the phone only; uninstalling removes it |

# Calling and Notifications

How the app calls a family member and how it shows a safety alert.

---

## 1. Calling a family member

### 1.1 The app prepares the call, it does not place it

The app uses **`Intent.ACTION_DIAL`**:

```java
Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + number));
context.startActivity(intent);
```

`ACTION_DIAL` opens the **dialler with the number already filled in**. The user
still has to press the green call button. This is deliberate:

* the user can see which number is being called,
* a wrong number does not call a stranger without a warning,
* the app does not need the `CALL_PHONE` permission.

`ACTION_CALL` would place the call immediately. It is **not used** in this app.

The code lives in `app/src/main/java/com/example/smartwashroom/util/DialerHelper.java`.

### 1.2 The number is cleaned up first

`PhoneNumberValidator.normalizeForDial()` removes everything that is not a digit
(spaces, dashes, brackets) but keeps a leading `+` for the country code.

| Saved in the app | Passed to the dialler |
|---|---|
| `+91 98765 43210` | `+919876543210` |
| `98765-43210` | `9876543210` |

### 1.3 The number is checked before it is saved

`PhoneNumberValidator.isValid()` accepts a number when

* it only contains digits and the characters `+`, space, `-`, `(`, `)`, and
* it contains between **5 and 15 digits**.

An empty name or an invalid number is refused in the form
(`ContactEditActivity`), and the reason is shown under the field.

### 1.4 Where the call button is used

| Screen | Button | What it does |
|---|---|---|
| Emergency Contacts | Call icon on every contact | Asks for confirmation, then opens the dialler |
| Alert warning screen | "Call a Family Member" | Shows a list of the saved contacts, then asks for confirmation, then opens the dialler |

If no contact is saved yet, the alert screen says so and offers a button that
opens the Emergency Contacts screen.

### 1.5 If the phone has no dialler

Some devices (for example a tablet without a SIM) have no dialler app. The app
catches the `ActivityNotFoundException` and shows:

> No phone dialler app was found on this device.

The app never crashes in that case.

### 1.6 Permissions

| Permission | Needed? | Why |
|---|---|---|
| `CALL_PHONE` | **No** | The app only opens the dialler, it never places a call |
| `android.hardware.telephony` | Declared as **not required** | The app must also install on devices without a SIM |

---

## 2. Notifications

### 2.1 What is shown today

When Demo Mode raises an alert, the app shows a **local notification** on the
phone:

* title: "No motion detected" or "Emergency button pressed"
* text: "Tap to open Smart Bathroom Safety and see the alert details."
* a small "DEMO" sub-title, so a simulated alert is never mistaken for a real one
* tapping the notification opens the **alert warning screen**

The code lives in `app/src/main/java/com/example/smartwashroom/ui/NotificationHelper.java`.

### 2.2 The notification channel

Android 8 (API 26) and newer need a *channel* for every kind of notification.
The channel is created in `SafetyApplication.onCreate()`, so it already exists
when the first alert is raised.

| Setting | Value | Why |
|---|---|---|
| Channel id | `safety_alerts` | Constant in `NotificationHelper` |
| Name | "Safety alerts" | Shown in the Android settings |
| Importance | `IMPORTANCE_HIGH` | The alert should be seen immediately |
| Show on lock screen | Yes | The user may not be looking at the phone |

### 2.3 The notification permission (Android 13+)

From Android 13 (API 33) an app must ask before it may show a notification:

```xml
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
```

The app asks for it on the first start (`BaseActivity.requestNotificationPermissionIfNeeded()`).
If the user refuses, the app still works - the alert is only shown inside the
app, and a short message explains that notifications are switched off.

### 2.4 What happens later with Firebase

When the real device sends an alert through Firebase, the notification is raised
by **Firebase Cloud Messaging (FCM)** instead of by the app itself:

```
Device  --->  Firebase Realtime Database  --->  app (reads the data)
       \-->  Firebase Cloud Messaging     --->  app (shows the notification)
```

The FCM service class would be something like this (it does **not** exist yet):

```java
public class SafetyFirebaseMessagingService extends FirebaseMessagingService {
    @Override
    public void onMessageReceived(RemoteMessage message) {
        AlertType type = AlertType.fromName(message.getData().get("type"));
        long timestamp = message.getData().containsKey("timestamp")
                ? Long.parseLong(message.getData().get("timestamp"))
                : System.currentTimeMillis();
        NotificationHelper.showAlertNotification(this, type, timestamp, false);
    }
}
```

The `NotificationHelper.showAlertNotification()` method is already written in a
way that both the demo mode and this future service can use, so no notification
code has to be written twice.

**Important:** the timestamp and the type must travel inside the **data** part of
the message, so that the app can show the correct time and the correct alert
screen.

---

## 3. Summary of the permissions used by the app

| Permission | Used for | Asked at runtime? |
|---|---|---|
| `POST_NOTIFICATIONS` | Safety alert notification (Android 13+) | Yes, at the first start |
| `INTERNET` | **Pending** - only needed for the Firebase integration | No |
| `ACCESS_NETWORK_STATE` | **Pending** - only needed for the Firebase integration | No |
| `CALL_PHONE` | **Not used** - the app only opens the dialler | No |

---

## 4. Troubleshooting

| Problem | Reason | Solution |
|---|---|---|
| No notification appears | The permission was refused, or the battery saver blocks it | Allow notifications for the app in the Android settings |
| Tapping the notification does nothing | The intent of the notification points to the alert screen | Check that `SafetyApplication` is registered in the manifest |
| "No phone dialler app was found" | The device has no dialler app | Use a device with a SIM, or test on an emulator with a dialer |
| The dialler opens with a wrong number | The number was saved with spaces or dashes | The app removes them automatically; check the saved contact |
| The call does not connect | The phone has no signal, or the number is wrong | Check the number on the contact screen |

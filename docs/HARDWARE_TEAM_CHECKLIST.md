# Hardware Team Checklist

This document collects the questions that the app team needs answered by the
hardware teammate. The app is finished on the phone side; these answers decide
what the device has to send and how the real alerts will reach the app.

**Current status:** the app works with simulated alerts (Demo Mode). No real
hardware is connected yet.

---

## 1. About the device

| # | Question | Why it matters | Answer |
|---|---|---|---|
| 1.1 | Which microcontroller / board is used? (ESP32, NodeMCU, Arduino, Raspberry Pi...) | Decides the firmware structure and the communication library | |
| 1.2 | Which motion sensor is used? (PIR, mmWave, ultrasonic, IMU + algorithm) | Decides how "no motion" is detected and how reliable it is | |
| 1.3 | Where is the sensor placed in the bathroom and how is it angled? | PIR sensors are very sensitive to the mounting angle | |
| 1.4 | Is there an accelerometer? What is the fall detection algorithm? | The app only shows "no motion"; a real fall algorithm may be better | |
| 1.5 | Is there a physical emergency button? Is it latching? | A latching button needs a "reset" step in the app | |
| 1.6 | How is the button connected (GPIO, separate board)? | Needed for the firmware | |
| 1.7 | How is the device powered? (mains adapter + backup battery / USB) | The app shows ONLINE / OFFLINE; a power cut must be visible | |
| 1.8 | Is there a battery level that can be reported? | Useful for the dashboard | |
| 1.9 | Is the device safe in a wet room? (IP rating, isolated supply) | Safety question for the college project | |
| 1.10 | Is there a status LED / buzzer on the device? | The user needs to know that the alert was really sent | |

---

## 2. About the communication with the phone

| # | Question | Why it matters | Answer |
|---|---|---|---|
| 2.1 | How does the device talk to the phone? (Wi-Fi + Firebase, Bluetooth, MQTT) | Decides the whole cloud design | |
| 2.2 | Which Wi-Fi does the device use - the home Wi-Fi or a phone hotspot? | The phone must be able to reach it | |
| 2.3 | How often does the device send a "I am alive" message? | This is the interval used for the ONLINE / OFFLINE decision | |
| 2.4 | How long may the phone stay "ONLINE" before it is shown as "OFFLINE"? | Needs a value that fits the real device | |
| 2.5 | Is the message encrypted? How is the key handled? | Important for personal safety data | |
| 2.6 | Can several phones be connected to one device? | Family members may each want the app | |
| 2.7 | Which Firebase project / Realtime Database does the device write to? | Must be the same project as the app | |

---

## 3. About the messages

The app already understands exactly these values (they are defined in
`app/src/main/java/com/example/smartwashroom/model/AlertType.java`):

| Value | Meaning |
|---|---|
| `NO_MOTION` | The device did not detect any movement |
| `EMERGENCY_BUTTON` | Somebody pressed the emergency button |

| # | Question | Answer |
|---|---|---|
| 3.1 | Are these two the only alert types the device will ever send? | |
| 3.2 | What is the exact JSON key for the alert type in the message? | |
| 3.3 | What is the exact JSON key for the timestamp, and in which unit? (milliseconds or seconds) | |
| 3.4 | Does the device send its own id, and what does it look like? | |
| 3.5 | Is the timestamp written by the device or by the phone? | |
| 3.6 | Can the device send more than one alert for the same event (retry)? | |
| 3.7 | How is an alert "cleared" once the person is fine? | |

---

## 4. About the Firebase part

The detailed list is in `docs/FIREBASE_INTEGRATION.md`. The short version that the
hardware team has to answer:

| # | Question | Answer |
|---|---|---|
| 4.1 | Firebase project ID and the Realtime Database URL of the device? | |
| 4.2 | Is `google-services.json` needed for the device firmware, or does it use another method (REST API, service account)? | |
| 4.3 | Which database paths does the device write to? | |
| 4.4 | Is Firebase Authentication used, and if yes, how does the device authenticate? | |
| 4.5 | Is FCM used for the notifications, or should the app poll the database? | |
| 4.6 | Who creates and manages the Firebase project? (Needed to avoid two different projects.) | |

---

## 5. Testing together

Please agree on the following before the demo:

- [ ] A test device that is always available for the demo
- [ ] A way to trigger "no motion" without waiting (for example a covered sensor
      or a hidden test button in the firmware)
- [ ] A way to trigger the emergency button
- [ ] A way to simulate a power cut / disconnection (to test the OFFLINE state)
- [ ] The agreed answer to "after how many minutes without a signal is the device
      OFFLINE?"
- [ ] A written list of the JSON messages the device sends (as a sample)

---

## 6. Handover checklist

When the integration starts, the hardware team should hand over:

1. The firmware source code (or at least the message building part)
2. A list of the JSON messages with example values
3. The Firebase project access (or a shared service account for the project)
4. A description of the error codes / status values the device can report
5. A short description of how the emergency button behaves (press, long press,
   latching, reset)

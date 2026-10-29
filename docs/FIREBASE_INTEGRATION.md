# Firebase Integration - Smart Washroom

**Status: COMPLETED & ACTIVE.** The Firebase Realtime Database and Cloud Messaging integration is fully implemented in the Android application.

---

## 1. Architecture Overview

```
Physical Hardware (ESP8266 / ESP32)
         |
         v
Firebase Realtime Database
   ├── /devices/{deviceId}       <- Live hardware telemetry & heartbeat
   ├── /alerts/{alertId}         <- Real emergency events log
   ├── /fcm_tokens/{tokenKey}    <- Registered family app devices
   ├── /family_contacts/{id}     <- Registered family members
   └── /notifications_log/{id}   <- Dedup & notification audit log
         |
         +-------------------------------------+
         |                                     |
         v                                     v
Firebase Cloud Function              Android Application (Java)
(Serverless push trigger)            ├── FirebaseAlertDataSource
         |                           ├── FirebaseRepository
         v (High-priority FCM)       ├── MainActivity (Live Dashboard)
Android System Notifications <-------└── SafetyFirebaseMessagingService
```

---

## 2. Realtime Database Structure & Schema

### 2.1 Hardware Telemetry Path: `/devices/{deviceId}`
Published periodically (every 5–15 seconds) by the hardware:
```json
{
  "online": true,
  "lastSeen": 1730104523000,
  "buzzer": false,
  "emergency": false,
  "motion": true,
  "occupancy": "OCCUPIED",
  "status": "NORMAL",
  "alert_type": null,
  "batteryLevel": 95
}
```

#### Field Specifications:
| Field | Types Supported | Meaning |
|---|---|---|
| `lastSeen` | Long (epoch ms or sec) | Hardware heartbeat timestamp. Used for offline detection |
| `online` | Boolean / Int (1/0) | Explicit online status reported by device |
| `buzzer` | Boolean / String ("ON"/"OFF") / Int | Whether the local bathroom buzzer is ringing |
| `emergency` | Boolean / String ("EMERGENCY"/"NORMAL") | True when fall or emergency button pressed |
| `motion` | Boolean / Int (1/0) | Motion/PIR/mmWave sensor detection |
| `occupancy` | String ("OCCUPIED"/"VACANT") | Room occupancy status |
| `status` | String ("NORMAL", "WARNING", "EMERGENCY") | Current overall system state |
| `alert_type` | String ("NO_MOTION", "EMERGENCY_BUTTON") | Specific active alert type |
| `batteryLevel` | Int (0–100) | Optional battery percentage |

---

## 3. Emergency Notification Logic

### Primary Rule:
> **When an emergency condition is detected by the hardware and the buzzer is NOT ringing, an emergency notification is sent to registered family members' mobile phones.**

### Implementation Details:
1. **Detection:** The app (`FirebaseAlertDataSource`) and backend Cloud Function both monitor `/devices/{deviceId}` in real time.
2. **Condition:** Checks if `emergency == true` AND `buzzer == false`.
3. **If Buzzer is ON:** The hardware buzzer is actively sounding locally; the buzzer-off specific notification is skipped, preserving local alarm behavior without generating false alerts.
4. **If Buzzer is OFF:** Emergency notification is triggered immediately:
   - High-priority Android notification is displayed with sound and vibration.
   - FCM push message is broadcast to all registered family devices via topic `washroom_alerts` and direct tokens.
   - Alert record is stored in `/alerts/{alertId}`.
5. **Deduplication:** Event signature `deviceId_timestamp_status` is checked before dispatching. Duplicate triggers for the same event are ignored.
6. **Re-arming:** When the emergency condition clears (`emergency == false`), the detector automatically re-arms for the next event.

---

## 4. Testing Scenarios Verified

- **Test A: Normal Operation** (`emergency=false, buzzer=false`) -> Dashboard NORMAL, no alert.
- **Test B: Emergency with Buzzer OFF** (`emergency=true, buzzer=false`) -> Emergency notification triggered!
- **Test C: Emergency with Buzzer ON** (`emergency=true, buzzer=true`) -> Preserves local buzzer, no buzzer-off alert.
- **Test D: Duplicate Updates** -> Prevented via event signature deduplication.
- **Test E: Hardware Offline** -> Transition to OFFLINE after configurable heartbeat timeout (default 60s); never falsely reported as emergency.
- **Test F: Flexible Format Parsing** -> Handles boolean, integer (0/1), and string representations seamlessly.
- **Test G: Multi-recipient FCM** -> Associates FCM tokens with multiple family contacts.

const functions = require("firebase-functions");
const admin = require("firebase-admin");

admin.initializeApp();

/**
 * Cloud Function triggered on hardware state changes at /devices/{deviceId}.
 *
 * Core requirement:
 * When an emergency condition is detected by the hardware and the buzzer is NOT ringing,
 * an emergency notification should be sent to the registered family members' mobile phones.
 */
exports.onHardwareDeviceUpdate = functions.database
    .ref("/devices/{deviceId}")
    .onWrite(async (change, context) => {
      const deviceId = context.params.deviceId;
      const after = change.after.val();
      const before = change.before.val() || {};

      if (!after) {
        // Device data deleted, nothing to do
        return null;
      }

      // 1. Extract and normalize fields
      const emergency = parseBoolean(after.emergency) ||
          parseBoolean(after.is_emergency) ||
          (after.status && after.status.toUpperCase() === "EMERGENCY") ||
          (after.status && after.status.toUpperCase() === "FALL");

      const buzzer = parseBoolean(after.buzzer) ||
          parseBoolean(after.buzzer_status) ||
          parseBoolean(after.is_buzzer);

      // Check if before state was already emergency to avoid re-triggering on unchanged state
      const wasEmergency = parseBoolean(before.emergency) ||
          (before.status && before.status.toUpperCase() === "EMERGENCY");
      const wasBuzzer = parseBoolean(before.buzzer);

      const timestamp = after.lastSeen || after.timestamp || Date.now();
      const alertType = after.alert_type || after.type || "EMERGENCY_BUTTON";

      // 2. Evaluate Core Emergency Condition: Emergency detected AND Buzzer is NOT ringing
      if (emergency && !buzzer) {
        // Deduplication: prevent repeated sends for identical event
        const eventId = `${deviceId}_${timestamp}_${alertType}`;
        const logRef = admin.database().ref(`notifications_log/${eventId}`);
        const logSnapshot = await logRef.once("value");

        if (logSnapshot.exists()) {
          console.log(`Notification for event ${eventId} already sent. Skipping duplicate.`);
          return null;
        }

        console.log(`EMERGENCY DETECTED on device ${deviceId} with Buzzer OFF! Dispatching notifications...`);

        // 3. Mark event in notifications_log immediately (Idempotency Lock)
        await logRef.set({
          deviceId: deviceId,
          timestamp: timestamp,
          alertType: alertType,
          buzzerStatus: "OFF",
          status: "DISPATCHING",
          createdAt: admin.database.ServerValue.TIMESTAMP,
        });

        // 4. Record the alert under /alerts so it appears in the Android alert history
        const alertRecord = {
          id: eventId,
          type: alertType,
          timestamp: timestamp,
          deviceId: deviceId,
          buzzer: false,
          demo: false,
          notificationSent: true,
          handled: false,
        };
        await admin.database().ref(`alerts/${eventId}`).set(alertRecord);

        // 5. Send FCM Push Notification to topic and registered device tokens
        const title = alertType === "NO_MOTION"
            ? "No Motion Detected!"
            : "Emergency Button Pressed!";
        const body = `Smart Washroom: Immediate assistance needed in ${deviceId}. Tap to view details and call family.`;

        const messagePayload = {
          topic: "washroom_alerts",
          notification: {
            title: title,
            body: body,
          },
          data: {
            type: String(alertType),
            timestamp: String(timestamp),
            deviceId: String(deviceId),
            buzzer: "false",
            demo: "false",
            title: title,
            body: body,
          },
          android: {
            priority: "high",
            notification: {
              channelId: "safety_alerts",
              priority: "max",
              defaultSound: true,
              defaultVibrateTimings: true,
            },
          },
        };

        try {
          const response = await admin.messaging().send(messagePayload);
          console.log("Successfully sent emergency FCM notification to topic: ", response);

          // Also deliver to individual registered family FCM tokens from /fcm_tokens
          const tokensSnapshot = await admin.database().ref("fcm_tokens").once("value");
          const tokens = [];
          tokensSnapshot.forEach((child) => {
            const token = child.child("token").val();
            if (token) tokens.push(token);
          });

          if (tokens.length > 0) {
            const multicastPayload = {
              tokens: tokens,
              notification: {
                title: title,
                body: body,
              },
              data: messagePayload.data,
              android: messagePayload.android,
            };
            const multicastResponse = await admin.messaging().sendEachForMulticast(multicastPayload);
            console.log(`Multicast FCM results: ${multicastResponse.successCount} sent, ${multicastResponse.failureCount} failed.`);
          }

          // Update log
          await logRef.update({
            status: "SENT",
            deliveredAt: Date.now(),
          });
        } catch (error) {
          console.error("Failed to send FCM notification: ", error);
          await logRef.update({
            status: "FAILED",
            error: error.message,
          });
        }
      } else if (emergency && buzzer) {
        console.log(`Emergency condition exists on ${deviceId}, but buzzer is ringing (ON). Preserving local alarm; skipping buzzer-OFF notification.`);
      }

      return null;
    });

/** Helper to parse booleans from various formats (boolean, number, string) */
function parseBoolean(val) {
  if (val === undefined || val === null) return false;
  if (typeof val === "boolean") return val;
  if (typeof val === "number") return val !== 0;
  const s = String(val).trim().toUpperCase();
  return s === "TRUE" || s === "ON" || s === "1" || s === "RINGING" || s === "YES";
}

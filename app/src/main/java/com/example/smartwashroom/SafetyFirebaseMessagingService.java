package com.example.smartwashroom;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;

import com.example.smartwashroom.data.FirebaseConfig;
import com.example.smartwashroom.model.AlertType;
import com.example.smartwashroom.ui.NotificationHelper;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.HashMap;
import java.util.Map;

/**
 * Handles Firebase Cloud Messaging (FCM) registration tokens and incoming emergency push notifications.
 *
 * <p>Receives emergency alerts triggered by the backend (Cloud Functions) or hardware
 * and raises high-priority notifications on the family member's phone even if the app
 * is closed or backgrounded.</p>
 */
public class SafetyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "SafetyFCMService";
    private static final String PREFS_FCM = "safety_fcm_prefs";
    private static final String KEY_FCM_TOKEN = "fcm_token";

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        Log.i(TAG, "New FCM Token received: " + token);

        saveTokenLocally(token);
        registerTokenWithFirebase(token);
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage message) {
        super.onMessageReceived(message);
        Log.i(TAG, "Received FCM message from: " + message.getFrom());

        // Extract data payload
        Map<String, String> data = message.getData();

        AlertType type = AlertType.EMERGENCY_BUTTON;
        if (data.containsKey("type")) {
            AlertType parsedType = AlertType.fromName(data.get("type"));
            if (parsedType != null) {
                type = parsedType;
            }
        }

        long timestamp = System.currentTimeMillis();
        if (data.containsKey("timestamp")) {
            try {
                timestamp = Long.parseLong(data.get("timestamp"));
            } catch (NumberFormatException ignored) {}
        }

        boolean demo = false;
        if (data.containsKey("demo")) {
            demo = Boolean.parseBoolean(data.get("demo"));
        }

        // Show lock-screen / heads-up notification
        NotificationHelper.showAlertNotification(this, type, timestamp, demo);
    }

    private void saveTokenLocally(String token) {
        SharedPreferences prefs = getSharedPreferences(PREFS_FCM, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_FCM_TOKEN, token).apply();
    }

    /**
     * Stores the FCM registration token under /fcm_tokens in Firebase Realtime Database
     * so that the Firebase Cloud Function can target this device during an emergency.
     */
    public static void registerTokenWithFirebase(String token) {
        if (token == null || token.isEmpty()) return;
        try {
            // Sanitize token for Firebase key (replace forbidden chars)
            String safeKey = token.replaceAll("[.#$\\[\\]/]", "_");
            if (safeKey.length() > 60) {
                safeKey = safeKey.substring(safeKey.length() - 60);
            }

            DatabaseReference tokenRef = FirebaseDatabase.getInstance()
                    .getReference(FirebaseConfig.PATH_FCM_TOKENS)
                    .child(safeKey);

            Map<String, Object> tokenInfo = new HashMap<>();
            tokenInfo.put("token", token);
            tokenInfo.put("deviceModel", Build.MANUFACTURER + " " + Build.MODEL);
            tokenInfo.put("androidVersion", Build.VERSION.RELEASE);
            tokenInfo.put("updatedAt", System.currentTimeMillis());

            tokenRef.setValue(tokenInfo);
            Log.d(TAG, "FCM token registered in Firebase Database at " + tokenRef.getPath());
        } catch (Exception e) {
            Log.e(TAG, "Failed to register FCM token with Firebase", e);
        }
    }

    public static String getLocalToken(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_FCM, Context.MODE_PRIVATE);
        return prefs.getString(KEY_FCM_TOKEN, null);
    }
}

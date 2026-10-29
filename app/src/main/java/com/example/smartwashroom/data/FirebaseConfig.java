package com.example.smartwashroom.data;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Configuration and path constants for Firebase Realtime Database and Cloud Messaging.
 */
public class FirebaseConfig {

    private static final String PREF_NAME = "firebase_config_prefs";
    private static final String KEY_DEVICE_ID = "key_device_id";
    private static final String KEY_HEARTBEAT_TIMEOUT_MS = "key_heartbeat_timeout_ms";
    private static final String KEY_CUSTOM_DB_URL = "key_custom_db_url";

    public static final String DEFAULT_DEVICE_ID = "washroom_01";
    public static final long DEFAULT_HEARTBEAT_TIMEOUT_MS = 60_000L; // 60 seconds

    public static final String PATH_DEVICES = "devices";
    public static final String PATH_ALERTS = "alerts";
    public static final String PATH_FCM_TOKENS = "fcm_tokens";
    public static final String PATH_NOTIFICATIONS_LOG = "notifications_log";
    public static final String PATH_FAMILY_CONTACTS = "family_contacts";
    public static final String FCM_TOPIC_EMERGENCY = "washroom_alerts";

    private final SharedPreferences preferences;

    public FirebaseConfig(Context context) {
        this.preferences = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public String getDeviceId() {
        return preferences.getString(KEY_DEVICE_ID, DEFAULT_DEVICE_ID);
    }

    public void setDeviceId(String deviceId) {
        preferences.edit().putString(KEY_DEVICE_ID, deviceId).apply();
    }

    public long getHeartbeatTimeoutMs() {
        return preferences.getLong(KEY_HEARTBEAT_TIMEOUT_MS, DEFAULT_HEARTBEAT_TIMEOUT_MS);
    }

    public void setHeartbeatTimeoutMs(long timeoutMs) {
        preferences.edit().putLong(KEY_HEARTBEAT_TIMEOUT_MS, timeoutMs).apply();
    }

    public static final String DEFAULT_DATABASE_URL = "https://iot-fall-detection-syste-8f255-default-rtdb.asia-southeast1.firebasedatabase.app";

    public String getCustomDatabaseUrl() {
        return preferences.getString(KEY_CUSTOM_DB_URL, DEFAULT_DATABASE_URL);
    }

    public void setCustomDatabaseUrl(String url) {
        preferences.edit().putString(KEY_CUSTOM_DB_URL, url).apply();
    }

    public static String getDevicePath(String deviceId) {
        return PATH_DEVICES + "/" + (deviceId != null ? deviceId : DEFAULT_DEVICE_ID);
    }
}

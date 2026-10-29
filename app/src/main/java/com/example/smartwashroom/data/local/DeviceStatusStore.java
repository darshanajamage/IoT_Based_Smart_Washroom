package com.example.smartwashroom.data.local;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.smartwashroom.model.AlertType;
import com.example.smartwashroom.model.DeviceStatus;

/**
 * Remembers the state of the safety device between two app starts.
 *
 * <p>Two things are stored separately on purpose:</p>
 * <ul>
 *     <li><b>connection status</b> - ONLINE or OFFLINE. It says whether we can
 *         talk to the device at all.</li>
 *     <li><b>active alert</b> - NO_MOTION or EMERGENCY (or none). It says that the
 *         device told us something is wrong.</li>
 * </ul>
 *
 * <p>Keeping them apart means the app can never report "no motion detected" as
 * "device offline", which would be a dangerous mistake.</p>
 */
public class DeviceStatusStore {

    private static final String PREFS_NAME = "device_status";
    private static final String KEY_CONNECTION = "connection_status";
    private static final String KEY_ACTIVE_ALERT = "active_alert_type";
    private static final String KEY_ACTIVE_ALERT_TIME = "active_alert_time";
    private static final String KEY_UPDATED_AT = "updated_at";

    private final SharedPreferences preferences;

    public DeviceStatusStore(Context context) {
        this.preferences = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    /** @return ONLINE (default) or OFFLINE. */
    public DeviceStatus getConnectionStatus() {
        String stored = preferences.getString(KEY_CONNECTION, DeviceStatus.ONLINE.name());
        return DeviceStatus.OFFLINE.name().equals(stored) ? DeviceStatus.OFFLINE : DeviceStatus.ONLINE;
    }

    /** Stores the connection status. Only ONLINE and OFFLINE are allowed here. */
    public void setConnectionStatus(DeviceStatus status) {
        DeviceStatus safeStatus = (status == DeviceStatus.OFFLINE) ? DeviceStatus.OFFLINE : DeviceStatus.ONLINE;
        preferences.edit()
                .putString(KEY_CONNECTION, safeStatus.name())
                .putLong(KEY_UPDATED_AT, System.currentTimeMillis())
                .apply();
    }

    /** @return the alert that is waiting to be handled, or {@code null}. */
    public AlertType getActiveAlertType() {
        String stored = preferences.getString(KEY_ACTIVE_ALERT, null);
        if (stored == null || stored.isEmpty()) {
            return null;
        }
        return AlertType.fromName(stored);
    }

    /** @return when the active alert was raised (0 when there is no active alert). */
    public long getActiveAlertTime() {
        return preferences.getLong(KEY_ACTIVE_ALERT_TIME, 0L);
    }

    /** Marks an alert as active so the dashboard can show it. */
    public void setActiveAlert(AlertType type, long timestampMillis) {
        if (type == null) {
            clearActiveAlert();
            return;
        }
        preferences.edit()
                .putString(KEY_ACTIVE_ALERT, type.name())
                .putLong(KEY_ACTIVE_ALERT_TIME, timestampMillis)
                .putLong(KEY_UPDATED_AT, System.currentTimeMillis())
                .apply();
    }

    /** The user handled the alert, so no alert is active any more. */
    public void clearActiveAlert() {
        preferences.edit()
                .remove(KEY_ACTIVE_ALERT)
                .remove(KEY_ACTIVE_ALERT_TIME)
                .apply();
    }

    /**
     * The status that should be shown on the dashboard.
     *
     * <p>An active alert always wins over the connection status, but the two are
     * still reported separately by {@link #getConnectionStatus()}.</p>
     */
    public DeviceStatus getDisplayStatus() {
        AlertType activeAlert = getActiveAlertType();
        DeviceStatus alertStatus = DeviceStatus.forAlertType(activeAlert);
        if (alertStatus != null) {
            return alertStatus;
        }
        return getConnectionStatus();
    }

    /** @return the last time anything changed, used for "last updated". */
    public long getUpdatedAt() {
        return preferences.getLong(KEY_UPDATED_AT, 0L);
    }
}

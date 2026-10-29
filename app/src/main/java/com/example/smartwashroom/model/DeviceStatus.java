package com.example.smartwashroom.model;

import com.example.smartwashroom.R;

/**
 * Every state the bathroom safety device can be in.
 *
 * <p>IMPORTANT: a {@link #NO_MOTION_ALERT} is <b>not</b> the same thing as an
 * {@link #OFFLINE} device. Offline means "the phone cannot reach the device",
 * while a no-motion alert means "the device is talking to us and says that
 * somebody did not move". The UI keeps them visually and logically separate.</p>
 */
public enum DeviceStatus {

    /** Device is connected and no alert is active. */
    ONLINE(R.string.status_online, R.drawable.ic_online, R.color.status_online, R.color.status_online_light),

    /** Device is not reachable (no power / no network). */
    OFFLINE(R.string.status_offline, R.drawable.ic_offline, R.color.status_offline, R.color.status_offline_light),

    /** An active "no motion detected" alert is waiting to be handled. */
    NO_MOTION_ALERT(R.string.status_no_motion_alert, R.drawable.ic_no_motion, R.color.alert_warning, R.color.alert_warning_light),

    /** An active "emergency button pressed" alert is waiting to be handled. */
    EMERGENCY_ALERT(R.string.status_emergency_alert, R.drawable.ic_emergency, R.color.alert_danger, R.color.alert_danger_light);

    private final int labelResId;
    private final int iconResId;
    private final int colorResId;
    private final int lightColorResId;

    DeviceStatus(int labelResId, int iconResId, int colorResId, int lightColorResId) {
        this.labelResId = labelResId;
        this.iconResId = iconResId;
        this.colorResId = colorResId;
        this.lightColorResId = lightColorResId;
    }

    public int getLabelResId() {
        return labelResId;
    }

    public int getIconResId() {
        return iconResId;
    }

    public int getColorResId() {
        return colorResId;
    }

    public int getLightColorResId() {
        return lightColorResId;
    }

    /** True for the two alert states, false for ONLINE and OFFLINE. */
    public boolean isAlert() {
        return this == NO_MOTION_ALERT || this == EMERGENCY_ALERT;
    }

    /** The status that represents the given alert type, or null for a null type. */
    public static DeviceStatus forAlertType(AlertType alertType) {
        if (alertType == null) {
            return null;
        }
        switch (alertType) {
            case NO_MOTION:
                return NO_MOTION_ALERT;
            case EMERGENCY_BUTTON:
                return EMERGENCY_ALERT;
            default:
                return null;
        }
    }
}

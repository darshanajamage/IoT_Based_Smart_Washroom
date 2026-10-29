package com.example.smartwashroom.model;

import com.example.smartwashroom.R;

/**
 * The two kinds of safety alerts that this app understands.
 *
 * <p>These values are pure data. They do not know anything about the screen that
 * shows them, which keeps the alert logic easy to test and easy to reuse when the
 * real device (and later Firebase) starts producing alerts.</p>
 */
public enum AlertType {

    /** The motion / inactivity sensor in the device did not see any movement. */
    NO_MOTION(
            R.string.alert_type_no_motion,
            R.string.alert_type_no_motion_short,
            R.string.alert_type_no_motion_explainer,
            R.drawable.ic_no_motion,
            R.color.alert_warning,
            R.color.alert_warning_light),

    /** The person physically pressed the emergency button on the device. */
    EMERGENCY_BUTTON(
            R.string.alert_type_emergency,
            R.string.alert_type_emergency_short,
            R.string.alert_type_emergency_explainer,
            R.drawable.ic_emergency,
            R.color.alert_danger,
            R.color.alert_danger_light);

    private final int titleResId;
    private final int shortTitleResId;
    private final int explainerResId;
    private final int iconResId;
    private final int colorResId;
    private final int lightColorResId;

    AlertType(int titleResId, int shortTitleResId, int explainerResId,
              int iconResId, int colorResId, int lightColorResId) {
        this.titleResId = titleResId;
        this.shortTitleResId = shortTitleResId;
        this.explainerResId = explainerResId;
        this.iconResId = iconResId;
        this.colorResId = colorResId;
        this.lightColorResId = lightColorResId;
    }

    /** Full title, for example "NO MOTION DETECTED". */
    public int getTitleResId() {
        return titleResId;
    }

    /** Short title, for example "No motion detected". */
    public int getShortTitleResId() {
        return shortTitleResId;
    }

    /** Sentence explaining what the user should do about this alert. */
    public int getExplainerResId() {
        return explainerResId;
    }

    public int getIconResId() {
        return iconResId;
    }

    /** Strong colour (text, icon, button background). */
    public int getColorResId() {
        return colorResId;
    }

    /** Soft colour (card background). */
    public int getLightColorResId() {
        return lightColorResId;
    }

    /**
     * Converts the enum constant name back into an {@link AlertType}.
     *
     * @return the matching type, or {@code null} when the name is unknown.
     */
    public static AlertType fromName(String name) {
        if (name == null) {
            return null;
        }
        for (AlertType type : values()) {
            if (type.name().equals(name)) {
                return type;
            }
        }
        return null;
    }
}

package com.example.smartwashroom.model;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.UUID;

/**
 * One entry of the alert history.
 *
 * <p>A record stores what happened, when it happened and whether it was produced
 * by Demo Mode (simulated) or by a real device. The demo flag is always stored so
 * the UI can show a clear "DEMO" label and a user can never confuse a simulated
 * alert with a real one.</p>
 */
public class AlertRecord {

    private final String id;
    private final AlertType type;
    private final long timestampMillis;
    private final boolean demo;

    public AlertRecord(String id, AlertType type, long timestampMillis, boolean demo) {
        this.id = (id == null || id.isEmpty()) ? UUID.randomUUID().toString() : id;
        this.type = type;
        this.timestampMillis = timestampMillis;
        this.demo = demo;
    }

    /** Convenience constructor for a record created right now. */
    public static AlertRecord create(AlertType type, boolean demo) {
        return new AlertRecord(UUID.randomUUID().toString(), type, System.currentTimeMillis(), demo);
    }

    public String getId() {
        return id;
    }

    public AlertType getType() {
        return type;
    }

    public long getTimestampMillis() {
        return timestampMillis;
    }

    public boolean isDemo() {
        return demo;
    }

    /** Converts this record into JSON so it can be stored on the phone. */
    public JSONObject toJson() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("id", id);
        json.put("type", type == null ? AlertType.NO_MOTION.name() : type.name());
        json.put("timestamp", timestampMillis);
        json.put("demo", demo);
        return json;
    }

    /**
     * Restores a record from JSON.
     *
     * @return the record, or {@code null} when the JSON is broken (a corrupt entry
     *         is skipped instead of crashing the app).
     */
    public static AlertRecord fromJson(JSONObject json) {
        if (json == null) {
            return null;
        }
        try {
            String id = json.optString("id", "");
            AlertType type = AlertType.fromName(json.optString("type", null));
            if (type == null) {
                return null;
            }
            long timestamp = json.optLong("timestamp", 0L);
            boolean demo = json.optBoolean("demo", false);
            return new AlertRecord(id, type, timestamp, demo);
        } catch (Exception e) {
            return null;
        }
    }
}

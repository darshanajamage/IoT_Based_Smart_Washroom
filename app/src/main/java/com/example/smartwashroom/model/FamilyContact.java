package com.example.smartwashroom.model;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * A family member that should be contacted and notified when an alert happens.
 *
 * <p>Supports local storage as well as FCM token association for emergency push alerts.</p>
 */
public class FamilyContact {

    private long id;
    private String name;
    private String phoneNumber;
    private String fcmToken;
    private boolean notificationsEnabled;

    public FamilyContact(long id, String name, String phoneNumber) {
        this(id, name, phoneNumber, null, true);
    }

    public FamilyContact(long id, String name, String phoneNumber, String fcmToken, boolean notificationsEnabled) {
        this.id = id;
        this.name = name == null ? "" : name.trim();
        this.phoneNumber = phoneNumber == null ? "" : phoneNumber.trim();
        this.fcmToken = fcmToken;
        this.notificationsEnabled = notificationsEnabled;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name == null ? "" : name.trim();
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber == null ? "" : phoneNumber.trim();
    }

    public String getFcmToken() {
        return fcmToken;
    }

    public void setFcmToken(String fcmToken) {
        this.fcmToken = fcmToken;
    }

    public boolean isNotificationsEnabled() {
        return notificationsEnabled;
    }

    public void setNotificationsEnabled(boolean notificationsEnabled) {
        this.notificationsEnabled = notificationsEnabled;
    }

    public JSONObject toJson() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("id", id);
        json.put("name", name);
        json.put("phone", phoneNumber);
        if (fcmToken != null) {
            json.put("fcmToken", fcmToken);
        }
        json.put("notificationsEnabled", notificationsEnabled);
        return json;
    }

    /** @return the restored contact, or {@code null} when the JSON is broken. */
    public static FamilyContact fromJson(JSONObject json) {
        if (json == null) {
            return null;
        }
        try {
            long id = json.optLong("id", 0L);
            String name = json.optString("name", "");
            String phone = json.optString("phone", "");
            String fcmToken = json.optString("fcmToken", null);
            boolean notificationsEnabled = json.optBoolean("notificationsEnabled", true);

            if (name.isEmpty() && phone.isEmpty()) {
                return null;
            }
            return new FamilyContact(id, name, phone, fcmToken, notificationsEnabled);
        } catch (Exception e) {
            return null;
        }
    }
}

package com.example.smartwashroom.data.local;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.smartwashroom.model.AlertRecord;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Stores the alert history on this phone.
 *
 * <p>Every alert that the app records (today: the ones created by Demo Mode) is
 * written here as JSON. The list is sorted newest first so the screens can simply
 * show the first item as "latest alert".</p>
 */
public class AlertHistoryStore {

    private static final String PREFS_NAME = "alert_history";
    private static final String KEY_ALERTS = "alerts_json";

    /** A safety limit so the local list can never grow without bounds. */
    private static final int MAX_STORED_ALERTS = 200;

    private final SharedPreferences preferences;

    public AlertHistoryStore(Context context) {
        this.preferences = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    /** @return every stored alert, newest first. */
    public List<AlertRecord> getAlerts() {
        List<AlertRecord> alerts = new ArrayList<>();
        String raw = preferences.getString(KEY_ALERTS, null);
        if (raw == null || raw.isEmpty()) {
            return alerts;
        }
        try {
            JSONArray array = new JSONArray(raw);
            for (int i = 0; i < array.length(); i++) {
                JSONObject json = array.optJSONObject(i);
                AlertRecord record = AlertRecord.fromJson(json);
                if (record != null) {
                    alerts.add(record);
                }
            }
        } catch (Exception e) {
            return new ArrayList<>();
        }
        return alerts;
    }

    /** Adds one alert to the top of the history. */
    public void addAlert(AlertRecord record) {
        if (record == null) {
            return;
        }
        List<AlertRecord> alerts = getAlerts();
        alerts.add(0, record);
        while (alerts.size() > MAX_STORED_ALERTS) {
            alerts.remove(alerts.size() - 1);
        }
        saveAlerts(alerts);
    }

    /** Deletes every locally stored alert. */
    public void clearAlerts() {
        preferences.edit().remove(KEY_ALERTS).apply();
    }

    /** @return the most recent alert, or {@code null} when the history is empty. */
    public AlertRecord getLatestAlert() {
        List<AlertRecord> alerts = getAlerts();
        if (alerts.isEmpty()) {
            return null;
        }
        return alerts.get(0);
    }

    private void saveAlerts(List<AlertRecord> alerts) {
        JSONArray array = new JSONArray();
        try {
            for (AlertRecord record : alerts) {
                array.put(record.toJson());
            }
        } catch (Exception e) {
            return;
        }
        preferences.edit().putString(KEY_ALERTS, array.toString()).apply();
    }
}

package com.example.smartwashroom.data;

import android.content.Context;

import com.example.smartwashroom.data.source.AlertDataSource;
import com.example.smartwashroom.data.source.DeviceStatusCallback;
import com.example.smartwashroom.data.source.FirebaseAlertDataSource;
import com.example.smartwashroom.data.source.HardwareStateCallback;
import com.example.smartwashroom.data.source.LocalDemoAlertDataSource;
import com.example.smartwashroom.data.source.RepositoryCallback;
import com.example.smartwashroom.model.AlertRecord;
import com.example.smartwashroom.model.DeviceStatus;
import com.example.smartwashroom.model.HardwareDeviceState;

import java.util.List;

/**
 * <h2>FirebaseRepository - the single entry point for alert and hardware data</h2>
 *
 * <p>Every screen in this app reads and writes alert data through this class.
 * Real hardware data from Firebase Realtime Database is streamed live to the UI.</p>
 */
public class FirebaseRepository {

    /** Status shown in the UI indicating live Firebase integration. */
    public static final String FIREBASE_INTEGRATION_STATUS = "ACTIVE";

    /** Explanation shown next to the "FIREBASE: ACTIVE" badge. */
    public static final String FIREBASE_INTEGRATION_NOTE =
            "Connected to Firebase Realtime Database. Live hardware status, emergency triggers, and notifications are active.";

    private static volatile FirebaseRepository instance;

    private final Context context;
    private final AlertDataSource dataSource;
    private final LocalDemoAlertDataSource demoDataSource;
    private final FirebaseConfig config;

    private FirebaseRepository(Context context, AlertDataSource dataSource) {
        this.context = context.getApplicationContext();
        this.dataSource = dataSource;
        this.demoDataSource = new LocalDemoAlertDataSource(context);
        this.config = new FirebaseConfig(context);
    }

    /**
     * @return the shared repository instance.
     */
    public static FirebaseRepository getInstance(Context context) {
        if (instance == null) {
            synchronized (FirebaseRepository.class) {
                if (instance == null) {
                    AlertDataSource realDataSource = createDataSource(context);
                    instance = new FirebaseRepository(context, realDataSource);
                }
            }
        }
        return instance;
    }

    /**
     * Creates the active Firebase data source.
     */
    private static AlertDataSource createDataSource(Context context) {
        return new FirebaseAlertDataSource(context);
    }

    /** @return true when a real Firebase data source is in use. */
    public boolean isFirebaseActive() {
        return (dataSource instanceof FirebaseAlertDataSource);
    }

    /**
     * Configures a custom Firebase Realtime Database URL.
     */
    public void setCustomDatabaseUrl(String url) {
        config.setCustomDatabaseUrl(url);
    }

    public String getCustomDatabaseUrl() {
        return config.getCustomDatabaseUrl();
    }

    public void setDeviceId(String deviceId) {
        config.setDeviceId(deviceId);
    }

    public String getDeviceId() {
        return config.getDeviceId();
    }

    public void setHeartbeatTimeoutMs(long timeoutMs) {
        config.setHeartbeatTimeoutMs(timeoutMs);
    }

    public long getHeartbeatTimeoutMs() {
        return config.getHeartbeatTimeoutMs();
    }

    /**
     * Returns the latest live hardware device state.
     */
    public HardwareDeviceState getHardwareDeviceState() {
        if (dataSource instanceof FirebaseAlertDataSource) {
            return ((FirebaseAlertDataSource) dataSource).getCurrentHardwareState();
        }
        return HardwareDeviceState.createDefault(config.getDeviceId());
    }

    /**
     * Subscribes to comprehensive live hardware updates (buzzer, emergency, motion, heartbeat).
     */
    public void observeHardwareState(HardwareStateCallback callback) {
        if (dataSource instanceof FirebaseAlertDataSource) {
            ((FirebaseAlertDataSource) dataSource).observeHardwareState(callback);
        } else if (callback != null) {
            callback.onHardwareStateChanged(HardwareDeviceState.createDefault(config.getDeviceId()));
        }
    }

    // ---------------------------------------------------------------------
    // Alert history
    // ---------------------------------------------------------------------

    /**
     * Stores one alert. If it is marked as demo, it also updates the demo data source.
     */
    public void saveAlert(AlertRecord record, RepositoryCallback<Void> callback) {
        if (record != null && record.isDemo()) {
            demoDataSource.saveAlert(record, null);
        }
        dataSource.saveAlert(record, callback);
    }

    /** Loads every stored alert (newest first). */
    public void loadAlertHistory(RepositoryCallback<List<AlertRecord>> callback) {
        dataSource.loadAlertHistory(callback);
    }

    /** Loads the most recent alert, or {@code null} when the history is empty. */
    public void loadLatestAlert(RepositoryCallback<AlertRecord> callback) {
        dataSource.loadLatestAlert(callback);
    }

    /** Deletes every stored alert. */
    public void clearAlertHistory(RepositoryCallback<Void> callback) {
        demoDataSource.clearAlertHistory(null);
        dataSource.clearAlertHistory(callback);
    }

    // ---------------------------------------------------------------------
    // Device status
    // ---------------------------------------------------------------------

    /** Starts watching the device status. */
    public void observeDeviceStatus(DeviceStatusCallback callback) {
        dataSource.observeDeviceStatus(callback);
    }

    /** Stops watching the device status. */
    public void stopObservingDeviceStatus() {
        dataSource.stopObservingDeviceStatus();
    }

    /** Sets the status that should be shown on the dashboard. */
    public void setDeviceStatus(DeviceStatus status, RepositoryCallback<Void> callback) {
        dataSource.setDeviceStatus(status, callback);
    }

    /**
     * @return the connection status of the device: ONLINE or OFFLINE.
     */
    public DeviceStatus getConnectionStatus() {
        return dataSource.getConnectionStatus();
    }

    /**
     * @return the status that should be shown on the dashboard.
     */
    public DeviceStatus getDisplayStatus() {
        return dataSource.getDisplayStatus();
    }

    /**
     * Marks the active alert as handled.
     */
    public void acknowledgeAlert(RepositoryCallback<Void> callback) {
        demoDataSource.acknowledgeAlert(null);
        dataSource.acknowledgeAlert(callback);
    }

    /** @return the last time the device status changed (0 when unknown). */
    public long getLastUpdatedAt() {
        return dataSource.getLastUpdatedAt();
    }
}

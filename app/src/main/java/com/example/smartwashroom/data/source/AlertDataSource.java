package com.example.smartwashroom.data.source;

import com.example.smartwashroom.model.AlertRecord;
import com.example.smartwashroom.model.DeviceStatus;

import java.util.List;

/**
 * The contract every alert data source must fulfil.
 *
 * <p>There are two implementations planned:</p>
 * <ol>
 *     <li>{@link LocalDemoAlertDataSource} - used today, it only reads and writes
 *         data on this phone.</li>
 *     <li>A Firebase Realtime Database implementation - <b>not written yet</b>.
 *         It will be added in the same package and plugged into
 *         {@link com.example.smartwashroom.data.FirebaseRepository}.</li>
 * </ol>
 *
 * <p>The screens never talk to a data source directly. They only use the
 * repository, so swapping the local source for Firebase later will not require
 * any change in the UI code.</p>
 */
public interface AlertDataSource {

    /** Stores one alert in the history. */
    void saveAlert(AlertRecord record, RepositoryCallback<Void> callback);

    /** Loads every stored alert (newest first). */
    void loadAlertHistory(RepositoryCallback<List<AlertRecord>> callback);

    /** Loads the most recent alert, or {@code null} when there is none. */
    void loadLatestAlert(RepositoryCallback<AlertRecord> callback);

    /** Deletes every stored alert. */
    void clearAlertHistory(RepositoryCallback<Void> callback);

    /** Sets the state that should be shown on the dashboard. */
    void setDeviceStatus(DeviceStatus status, RepositoryCallback<Void> callback);

    /**
     * @return the connection status of the device: ONLINE or OFFLINE.
     *         This is never an alert state - an alert and an offline device are
     *         two different things.
     */
    DeviceStatus getConnectionStatus();

    /**
     * @return the status that should be shown on the dashboard. When an alert is
     *         active the alert status wins, otherwise the connection status is
     *         returned.
     */
    DeviceStatus getDisplayStatus();

    /**
     * Marks the active alert as handled. The alert stays in the history, only the
     * "there is an alert right now" state is cleared.
     */
    void acknowledgeAlert(RepositoryCallback<Void> callback);

    /** @return the last time the device status changed (0 when unknown). */
    long getLastUpdatedAt();

    /**
     * Starts watching the device status.
     *
     * <p>For the local data source the current value is delivered immediately.
     * For the Firebase data source this will become a
     * {@code ValueEventListener} on a database path that the Firebase team
     * confirms later.</p>
     */
    void observeDeviceStatus(DeviceStatusCallback callback);

    /** Stops watching the device status. */
    void stopObservingDeviceStatus();
}

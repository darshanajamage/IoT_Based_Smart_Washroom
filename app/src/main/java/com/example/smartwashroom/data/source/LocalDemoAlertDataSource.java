package com.example.smartwashroom.data.source;

import android.content.Context;

import com.example.smartwashroom.data.local.AlertHistoryStore;
import com.example.smartwashroom.data.local.DeviceStatusStore;
import com.example.smartwashroom.model.AlertRecord;
import com.example.smartwashroom.model.AlertType;
import com.example.smartwashroom.model.DeviceStatus;

import java.util.List;

/**
 * The data source that is used today: everything stays on this phone.
 *
 * <p>It is called "demo" because the alerts it stores are produced by Demo Mode.
 * When the Firebase implementation is ready it will be added next to this class
 * and the repository will be pointed at it. Nothing in this class needs to
 * change when that happens.</p>
 */
public class LocalDemoAlertDataSource implements AlertDataSource {

    private final AlertHistoryStore alertHistoryStore;
    private final DeviceStatusStore deviceStatusStore;

    public LocalDemoAlertDataSource(Context context) {
        this.alertHistoryStore = new AlertHistoryStore(context);
        this.deviceStatusStore = new DeviceStatusStore(context);
    }

    @Override
    public void saveAlert(AlertRecord record, RepositoryCallback<Void> callback) {
        try {
            alertHistoryStore.addAlert(record);
            if (record != null && record.getType() != null) {
                // An alert is always raised while the device is online.
                deviceStatusStore.setConnectionStatus(DeviceStatus.ONLINE);
                deviceStatusStore.setActiveAlert(record.getType(), record.getTimestampMillis());
            }
            if (callback != null) {
                callback.onSuccess(null);
            }
        } catch (Exception e) {
            if (callback != null) {
                callback.onError(e);
            }
        }
    }

    @Override
    public void loadAlertHistory(RepositoryCallback<List<AlertRecord>> callback) {
        try {
            if (callback != null) {
                callback.onSuccess(alertHistoryStore.getAlerts());
            }
        } catch (Exception e) {
            if (callback != null) {
                callback.onError(e);
            }
        }
    }

    @Override
    public void loadLatestAlert(RepositoryCallback<AlertRecord> callback) {
        try {
            if (callback != null) {
                callback.onSuccess(alertHistoryStore.getLatestAlert());
            }
        } catch (Exception e) {
            if (callback != null) {
                callback.onError(e);
            }
        }
    }

    @Override
    public void clearAlertHistory(RepositoryCallback<Void> callback) {
        try {
            alertHistoryStore.clearAlerts();
            deviceStatusStore.clearActiveAlert();
            if (callback != null) {
                callback.onSuccess(null);
            }
        } catch (Exception e) {
            if (callback != null) {
                callback.onError(e);
            }
        }
    }

    @Override
    public void setDeviceStatus(DeviceStatus status, RepositoryCallback<Void> callback) {
        try {
            if (status != null && status.isAlert()) {
                // Alert states are raised through saveAlert(), never set directly.
                deviceStatusStore.setActiveAlert(alertTypeOf(status), System.currentTimeMillis());
            } else if (status == DeviceStatus.OFFLINE) {
                deviceStatusStore.setConnectionStatus(DeviceStatus.OFFLINE);
            } else {
                deviceStatusStore.setConnectionStatus(DeviceStatus.ONLINE);
                deviceStatusStore.clearActiveAlert();
            }
            if (callback != null) {
                callback.onSuccess(null);
            }
        } catch (Exception e) {
            if (callback != null) {
                callback.onError(e);
            }
        }
    }

    @Override
    public DeviceStatus getConnectionStatus() {
        return deviceStatusStore.getConnectionStatus();
    }

    @Override
    public DeviceStatus getDisplayStatus() {
        return deviceStatusStore.getDisplayStatus();
    }

    @Override
    public void acknowledgeAlert(RepositoryCallback<Void> callback) {
        try {
            deviceStatusStore.clearActiveAlert();
            if (callback != null) {
                callback.onSuccess(null);
            }
        } catch (Exception e) {
            if (callback != null) {
                callback.onError(e);
            }
        }
    }

    @Override
   public long getLastUpdatedAt() {
        return deviceStatusStore.getUpdatedAt();
    }

    @Override
    public void observeDeviceStatus(DeviceStatusCallback callback) {
        if (callback != null) {
            callback.onDeviceStatusChanged(deviceStatusStore.getDisplayStatus());
        }
    }

    @Override
    public void stopObservingDeviceStatus() {
        // Nothing to stop: the local data source does not keep a live connection.
    }

    /** @return the alert type behind an alert status, or null. */
    private AlertType alertTypeOf(DeviceStatus status) {
        if (status == DeviceStatus.NO_MOTION_ALERT) {
            return AlertType.NO_MOTION;
        }
        if (status == DeviceStatus.EMERGENCY_ALERT) {
            return AlertType.EMERGENCY_BUTTON;
        }
        return null;
    }
}

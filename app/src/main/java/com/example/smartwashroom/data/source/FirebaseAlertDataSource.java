package com.example.smartwashroom.data.source;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;

import com.example.smartwashroom.data.FirebaseConfig;
import com.example.smartwashroom.data.local.AlertHistoryStore;
import com.example.smartwashroom.data.local.DeviceStatusStore;
import com.example.smartwashroom.model.AlertRecord;
import com.example.smartwashroom.model.AlertType;
import com.example.smartwashroom.model.DeviceStatus;
import com.example.smartwashroom.model.HardwareDeviceState;
import com.example.smartwashroom.ui.NotificationHelper;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Live Firebase Realtime Database implementation of {@link AlertDataSource}.
 *
 * <p>Connects to the hardware Firebase database, watches hardware status and heartbeats,
 * evaluates emergency conditions, and triggers notifications when an emergency condition
 * exists and the buzzer is NOT ringing.</p>
 */
public class FirebaseAlertDataSource implements AlertDataSource {

    private static final String TAG = "FirebaseAlertDataSource";

    private final Context context;
    private final FirebaseConfig config;
    private final AlertHistoryStore alertHistoryStore;
    private final DeviceStatusStore deviceStatusStore;
    private final Handler mainHandler;

    private FirebaseDatabase database;
    private DatabaseReference deviceRef;
    private DatabaseReference alertsRef;
    private DatabaseReference connectionRef;

    private ValueEventListener deviceListener;
    private ValueEventListener connectionListener;

    private DeviceStatusCallback statusCallback;
    private HardwareStateCallback hardwareCallback;

    private HardwareDeviceState currentHardwareState;
    private DeviceStatus currentConnectionStatus = DeviceStatus.OFFLINE;
    private DeviceStatus currentDisplayStatus = DeviceStatus.OFFLINE;
    private long lastUpdatedAt = 0L;

    // Deduplication and re-arming state
    private boolean alertArmed = true;
    private long lastTriggeredEventTime = 0L;
    private String lastTriggeredEventSignature = "";

    // Periodic heartbeat checker
    private final Runnable heartbeatCheckerRunnable = new Runnable() {
        @Override
        public void run() {
            checkHeartbeatStaleness();
            mainHandler.postDelayed(this, 5000L); // Check every 5 seconds
        }
    };

    public FirebaseAlertDataSource(Context context) {
        this.context = context.getApplicationContext();
        this.config = new FirebaseConfig(context);
        this.alertHistoryStore = new AlertHistoryStore(context);
        this.deviceStatusStore = new DeviceStatusStore(context);
        this.mainHandler = new Handler(Looper.getMainLooper());

        this.currentHardwareState = HardwareDeviceState.createDefault(config.getDeviceId());
        initFirebase();
        startHeartbeatChecker();
    }

    private void initFirebase() {
        try {
            String customUrl = config.getCustomDatabaseUrl();
            if (customUrl != null && !customUrl.trim().isEmpty()) {
                database = FirebaseDatabase.getInstance(customUrl.trim());
            } else {
                database = FirebaseDatabase.getInstance();
            }

            String deviceId = config.getDeviceId();
            deviceRef = database.getReference(FirebaseConfig.getDevicePath(deviceId));
            alertsRef = database.getReference(FirebaseConfig.PATH_ALERTS);
            connectionRef = database.getReference(".info/connected");

            listenToConnectionStatus();
            listenToHardwareDevice();
        } catch (Exception e) {
            Log.e(TAG, "Error initializing Firebase Database", e);
        }
    }

    private void startHeartbeatChecker() {
        mainHandler.postDelayed(heartbeatCheckerRunnable, 5000L);
    }

    /**
     * Listens to Firebase client network connection (.info/connected).
     */
    private void listenToConnectionStatus() {
        if (connectionRef == null) return;

        connectionListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Boolean connected = snapshot.getValue(Boolean.class);
                if (connected == null || !connected) {
                    Log.d(TAG, "Phone lost connection to Firebase");
                } else {
                    Log.d(TAG, "Phone connected to Firebase");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.w(TAG, "Connection listener cancelled: " + error.getMessage());
            }
        };
        connectionRef.addValueEventListener(connectionListener);
    }

    /**
     * Listens in real time to the hardware device node at /devices/{deviceId}.
     */
    private void listenToHardwareDevice() {
        if (deviceRef == null) return;

        deviceListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                processHardwareDataSnapshot(snapshot);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Database listener cancelled: " + error.getMessage() + " (Code: " + error.getCode() + ")");
                if (hardwareCallback != null) {
                    hardwareCallback.onError(error.toException());
                }
            }
        };
        deviceRef.addValueEventListener(deviceListener);
    }

    /**
     * Processes live hardware snapshot, parses state, updates heartbeat and evaluates emergency logic.
     */
    private void processHardwareDataSnapshot(DataSnapshot snapshot) {
        long timeoutMs = config.getHeartbeatTimeoutMs();
        HardwareDeviceState newState = HardwareDeviceState.fromSnapshot(snapshot, timeoutMs);

        currentHardwareState = newState;
        lastUpdatedAt = newState.getLastSeen() > 0 ? newState.getLastSeen() : System.currentTimeMillis();

        // Update connection status based on hardware heartbeat
        currentConnectionStatus = newState.isOnline() ? DeviceStatus.ONLINE : DeviceStatus.OFFLINE;

        // Core Requirement: Evaluate Emergency Condition with Buzzer Check
        evaluateEmergencyLogic(newState);

        // Update display status
        if (newState.getAlertType() != null) {
            currentDisplayStatus = DeviceStatus.forAlertType(newState.getAlertType());
        } else if (newState.isEmergencyActive()) {
            currentDisplayStatus = DeviceStatus.EMERGENCY_ALERT;
        } else {
            currentDisplayStatus = currentConnectionStatus;
        }

        // Cache in local store
        deviceStatusStore.setConnectionStatus(currentConnectionStatus);
        if (currentDisplayStatus.isAlert()) {
            deviceStatusStore.setActiveAlert(newState.getAlertType() != null ? newState.getAlertType() : AlertType.EMERGENCY_BUTTON, lastUpdatedAt);
        } else {
            deviceStatusStore.clearActiveAlert();
        }

        // Notify UI callbacks
        notifyCallbacks();
    }

    /**
     * Evaluates the live hardware state for emergency conditions.
     *
     * Records an alert and updates history whenever emergency is detected.
     * The "buzzer OFF" sub-condition only controls whether a push notification
     * is dispatched (buzzer ON = local hardware is already alerting loudly).
     * Either way, the event is always written to alert history and latest alert.
     */
    public void evaluateEmergencyLogic(HardwareDeviceState state) {
        if (state == null) return;

        boolean isEmergency = state.isEmergencyActive();
        boolean isBuzzerRinging = state.isBuzzerActive();

        if (isEmergency) {
            // Build a deduplication key: device + timestamp + alert type
            AlertType type = state.getAlertType() != null ? state.getAlertType() : AlertType.EMERGENCY_BUTTON;
            long timestamp = state.getLastSeen() > 0 ? state.getLastSeen() : System.currentTimeMillis();
            String eventSignature = state.getDeviceId() + "_" + timestamp + "_" + type.name();

            if (alertArmed && !eventSignature.equals(lastTriggeredEventSignature)) {
                lastTriggeredEventSignature = eventSignature;
                lastTriggeredEventTime = System.currentTimeMillis();
                alertArmed = false; // Disarm until emergency clears

                Log.w(TAG, "EMERGENCY DETECTED! Type=" + type + " Buzzer=" + isBuzzerRinging);

                // 1. Always persist the alert record to history and Firebase /alerts
                AlertRecord record = new AlertRecord(
                        java.util.UUID.randomUUID().toString(),
                        type,
                        timestamp,
                        false // Live hardware alert, never marked as demo
                );
                saveAlert(record, null);

                // 2. Show in-app notification on the phone
                NotificationHelper.showAlertNotification(context, type, timestamp, false);

                // 3. Log in Firebase only when buzzer is OFF (buzzer ON = hardware handles local alert)
                if (!isBuzzerRinging) {
                    Log.w(TAG, "Buzzer is OFF - dispatching push notification for family.");
                    recordNotificationLog(record.getId(), state.getDeviceId(), "SENT", timestamp);
                } else {
                    Log.i(TAG, "Buzzer is ON - local hardware alerting. Push notification skipped.");
                    recordNotificationLog(record.getId(), state.getDeviceId(), "SKIPPED_BUZZER_ON", timestamp);
                }

                // 4. Notify UI immediately so Latest Alert card updates without waiting for next refresh
                notifyCallbacks();
            }
        } else if (!isEmergency) {
            // Re-arm for next event when emergency clears
            if (!alertArmed) {
                Log.i(TAG, "Emergency cleared. Alert system re-armed.");
            }
            alertArmed = true;
        }
    }

    private void recordNotificationLog(String alertId, String deviceId, String status, long timestamp) {
        if (database == null) return;
        try {
            DatabaseReference logRef = database.getReference(FirebaseConfig.PATH_NOTIFICATIONS_LOG).push();
            Map<String, Object> logEntry = new HashMap<>();
            logEntry.put("alertId", alertId);
            logEntry.put("deviceId", deviceId);
            logEntry.put("status", status);
            logEntry.put("timestamp", timestamp);
            logEntry.put("reason", "EMERGENCY_WITH_BUZZER_OFF");
            logRef.setValue(logEntry);
        } catch (Exception e) {
            Log.e(TAG, "Failed to write notification log to Firebase", e);
        }
    }

    /**
     * Checks whether the hardware heartbeat has timed out and transitions to OFFLINE if stale.
     */
    private void checkHeartbeatStaleness() {
        if (currentHardwareState == null || currentHardwareState.getLastSeen() == 0) {
            return;
        }

        long now = System.currentTimeMillis();
        long diff = now - currentHardwareState.getLastSeen();
        long timeoutMs = config.getHeartbeatTimeoutMs();

        if (diff > timeoutMs && currentConnectionStatus == DeviceStatus.ONLINE) {
            Log.i(TAG, "Hardware heartbeat timed out (" + diff + "ms > " + timeoutMs + "ms). Transitioning to OFFLINE.");
            currentConnectionStatus = DeviceStatus.OFFLINE;

            // Reconstruct state as offline
            currentHardwareState = new HardwareDeviceState(
                    currentHardwareState.getDeviceId(),
                    currentHardwareState.getLastSeen(),
                    false, // online = false
                    currentHardwareState.isBuzzerActive(),
                    currentHardwareState.isEmergencyActive(),
                    false, // motion = false
                    "UNKNOWN",
                    currentHardwareState.getAlertType(),
                    currentHardwareState.getBatteryLevel(),
                    "OFFLINE"
            );

            if (!currentDisplayStatus.isAlert()) {
                currentDisplayStatus = DeviceStatus.OFFLINE;
            }

            deviceStatusStore.setConnectionStatus(DeviceStatus.OFFLINE);
            notifyCallbacks();
        }
    }

    private void notifyCallbacks() {
        if (statusCallback != null) {
            statusCallback.onDeviceStatusChanged(currentDisplayStatus);
        }
        if (hardwareCallback != null) {
            hardwareCallback.onHardwareStateChanged(currentHardwareState);
        }
    }

    public HardwareDeviceState getCurrentHardwareState() {
        return currentHardwareState;
    }

    public void observeHardwareState(HardwareStateCallback callback) {
        this.hardwareCallback = callback;
        if (callback != null && currentHardwareState != null) {
            callback.onHardwareStateChanged(currentHardwareState);
        }
    }

    // ---------------------------------------------------------------------
    // AlertDataSource implementation
    // ---------------------------------------------------------------------

    @Override
    public void saveAlert(AlertRecord record, RepositoryCallback<Void> callback) {
        try {
            // Save locally first so the Latest Alert card can update instantly
            alertHistoryStore.addAlert(record);

            // Immediately notify UI callbacks so Latest Alert card refreshes without delay
            mainHandler.post(() -> notifyCallbacks());

            // Save to Firebase Realtime Database
            if (alertsRef != null && record != null) {
                Map<String, Object> alertMap = new HashMap<>();
                alertMap.put("id", record.getId());
                alertMap.put("type", record.getType() != null ? record.getType().name() : "EMERGENCY_BUTTON");
                alertMap.put("timestamp", record.getTimestampMillis());
                alertMap.put("demo", record.isDemo());
                alertMap.put("deviceId", config.getDeviceId());
                alertMap.put("buzzer", currentHardwareState != null && currentHardwareState.isBuzzerActive());
                alertMap.put("notificationSent", true);

                alertsRef.child(record.getId()).setValue(alertMap)
                        .addOnSuccessListener(unused -> {
                            Log.i(TAG, "Alert saved to Firebase /alerts: " + record.getId());
                            if (callback != null) callback.onSuccess(null);
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Firebase saveAlert failed", e);
                            if (callback != null) callback.onError(e);
                        });
            } else {
                if (callback != null) callback.onSuccess(null);
            }
        } catch (Exception e) {
            if (callback != null) callback.onError(e);
        }
    }

    @Override
    public void loadAlertHistory(RepositoryCallback<List<AlertRecord>> callback) {
        if (alertsRef == null) {
            // Fall back to local store
            if (callback != null) callback.onSuccess(alertHistoryStore.getAlerts());
            return;
        }

        alertsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<AlertRecord> records = new ArrayList<>();
                for (DataSnapshot child : snapshot.getChildren()) {
                    String id = child.child("id").getValue(String.class);
                    if (id == null) id = child.getKey();

                    String typeStr = child.child("type").getValue(String.class);
                    AlertType type = AlertType.fromName(typeStr);
                    if (type == null) type = AlertType.EMERGENCY_BUTTON;

                    Long ts = child.child("timestamp").getValue(Long.class);
                    long timestamp = ts != null ? ts : 0L;

                    Boolean isDemo = child.child("demo").getValue(Boolean.class);
                    boolean demo = isDemo != null && isDemo;

                    records.add(new AlertRecord(id, type, timestamp, demo));
                }

                // If Firebase has records, sort newest first
                if (!records.isEmpty()) {
                    Collections.sort(records, (a, b) -> Long.compare(b.getTimestampMillis(), a.getTimestampMillis()));
                    if (callback != null) callback.onSuccess(records);
                } else {
                    // Fall back to local history store if Firebase is empty
                    if (callback != null) callback.onSuccess(alertHistoryStore.getAlerts());
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.w(TAG, "loadAlertHistory cancelled, reading local store: " + error.getMessage());
                if (callback != null) callback.onSuccess(alertHistoryStore.getAlerts());
            }
        });
    }

    @Override
    public void loadLatestAlert(RepositoryCallback<AlertRecord> callback) {
        // Return from local store immediately for instant UI refresh.
        // Local store is always written first in saveAlert before Firebase completes.
        AlertRecord localLatest = alertHistoryStore.getLatestAlert();
        if (localLatest != null) {
            if (callback != null) callback.onSuccess(localLatest);
            return;
        }

        // Fallback: query Firebase if local store is empty (e.g. fresh install)
        loadAlertHistory(new RepositoryCallback<List<AlertRecord>>() {
            @Override
            public void onSuccess(List<AlertRecord> result) {
                if (result != null && !result.isEmpty()) {
                    if (callback != null) callback.onSuccess(result.get(0));
                } else {
                    if (callback != null) callback.onSuccess(null);
                }
            }

            @Override
            public void onError(Exception error) {
                if (callback != null) callback.onSuccess(null);
            }
        });
    }

    @Override
    public void clearAlertHistory(RepositoryCallback<Void> callback) {
        alertHistoryStore.clearAlerts();
        deviceStatusStore.clearActiveAlert();

        if (alertsRef != null) {
            alertsRef.removeValue()
                    .addOnSuccessListener(unused -> {
                        if (callback != null) callback.onSuccess(null);
                    })
                    .addOnFailureListener(e -> {
                        if (callback != null) callback.onError(e);
                    });
        } else {
            if (callback != null) callback.onSuccess(null);
        }
    }

    @Override
    public void setDeviceStatus(DeviceStatus status, RepositoryCallback<Void> callback) {
        if (status == DeviceStatus.OFFLINE) {
            currentConnectionStatus = DeviceStatus.OFFLINE;
            currentDisplayStatus = DeviceStatus.OFFLINE;
        } else if (status == DeviceStatus.ONLINE) {
            currentConnectionStatus = DeviceStatus.ONLINE;
            currentDisplayStatus = DeviceStatus.ONLINE;
            alertArmed = true; // Re-arm
        }

        if (deviceRef != null) {
            Map<String, Object> update = new HashMap<>();
            update.put("online", status == DeviceStatus.ONLINE);
            update.put("status", status.name());
            update.put("lastSeen", System.currentTimeMillis());
            deviceRef.updateChildren(update);
        }

        notifyCallbacks();
        if (callback != null) callback.onSuccess(null);
    }

    @Override
    public DeviceStatus getConnectionStatus() {
        return currentConnectionStatus;
    }

    @Override
    public DeviceStatus getDisplayStatus() {
        return currentDisplayStatus;
    }

    @Override
    public void acknowledgeAlert(RepositoryCallback<Void> callback) {
        alertArmed = true; // Re-arm for subsequent events
        deviceStatusStore.clearActiveAlert();

        if (currentHardwareState != null) {
            currentHardwareState = new HardwareDeviceState(
                    currentHardwareState.getDeviceId(),
                    System.currentTimeMillis(),
                    currentHardwareState.isOnline(),
                    false, // buzzer off
                    false, // emergency resolved
                    currentHardwareState.isMotionDetected(),
                    currentHardwareState.getOccupancy(),
                    null,
                    currentHardwareState.getBatteryLevel(),
                    "NORMAL"
            );
        }

        currentDisplayStatus = currentConnectionStatus;

        if (deviceRef != null) {
            Map<String, Object> update = new HashMap<>();
            update.put("emergency", false);
            update.put("buzzer", false);
            update.put("status", "NORMAL");
            update.put("acknowledgedAt", System.currentTimeMillis());
            deviceRef.updateChildren(update)
                    .addOnSuccessListener(unused -> {
                        notifyCallbacks();
                        if (callback != null) callback.onSuccess(null);
                    })
                    .addOnFailureListener(e -> {
                        notifyCallbacks();
                        if (callback != null) callback.onSuccess(null);
                    });
        } else {
            notifyCallbacks();
            if (callback != null) callback.onSuccess(null);
        }
    }

    @Override
    public long getLastUpdatedAt() {
        return lastUpdatedAt;
    }

    @Override
    public void observeDeviceStatus(DeviceStatusCallback callback) {
        this.statusCallback = callback;
        if (callback != null) {
            callback.onDeviceStatusChanged(currentDisplayStatus);
        }
    }

    @Override
    public void stopObservingDeviceStatus() {
        this.statusCallback = null;
        this.hardwareCallback = null;
    }

    public void cleanup() {
        mainHandler.removeCallbacks(heartbeatCheckerRunnable);
        if (deviceRef != null && deviceListener != null) {
            deviceRef.removeEventListener(deviceListener);
        }
        if (connectionRef != null && connectionListener != null) {
            connectionRef.removeEventListener(connectionListener);
        }
    }
}

package com.example.smartwashroom.ui;

import android.app.AlertDialog;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.example.smartwashroom.R;
import com.example.smartwashroom.data.FirebaseConfig;
import com.example.smartwashroom.data.FirebaseRepository;
import com.example.smartwashroom.data.source.RepositoryCallback;
import com.example.smartwashroom.model.AlertRecord;
import com.example.smartwashroom.model.AlertType;
import com.example.smartwashroom.model.DeviceStatus;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

/**
 * DEMO & HARDWARE TEST MODE screen.
 *
 * <p>Allows testing simulated alerts, configuring hardware device ID, and triggering
 * live Firebase hardware events to test the physical hardware integration end-to-end.</p>
 */
public class DemoModeActivity extends BaseActivity {

    private FirebaseRepository repository;
    private MaterialButton connectionButton;
    private TextView configDeviceIdDescText;

    @Override
    protected int getLayoutResourceId() {
        return R.layout.activity_demo_mode;
    }

    @Override
    protected String getToolbarTitle() {
        return getString(R.string.demo_title);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        repository = FirebaseRepository.getInstance(this);

        connectionButton = findViewById(R.id.btn_toggle_connection);
        configDeviceIdDescText = findViewById(R.id.tv_config_device_id_desc);

        // Local simulation buttons
        findViewById(R.id.btn_simulate_no_motion).setOnClickListener(view -> triggerDemoAlert(AlertType.NO_MOTION));
        findViewById(R.id.btn_simulate_emergency).setOnClickListener(view -> triggerDemoAlert(AlertType.EMERGENCY_BUTTON));
        connectionButton.setOnClickListener(view -> toggleConnection());

        // Hardware test & configuration buttons
        findViewById(R.id.btn_config_device).setOnClickListener(view -> showConfigDialog());
        findViewById(R.id.btn_test_hardware_buzzer_off).setOnClickListener(view -> testLiveHardwareEmergency(false));
        findViewById(R.id.btn_test_hardware_buzzer_on).setOnClickListener(view -> testLiveHardwareEmergency(true));

        updateDeviceIdDescription();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateConnectionButton();
        updateDeviceIdDescription();
    }

    private void updateDeviceIdDescription() {
        if (configDeviceIdDescText != null) {
            configDeviceIdDescText.setText(getString(R.string.demo_config_device_id_desc, repository.getDeviceId()));
        }
    }

    /**
     * Writes a live hardware test event directly to Firebase Realtime Database at /devices/{deviceId}.
     *
     * @param buzzerActive true for buzzer ON, false for buzzer OFF.
     */
    private void testLiveHardwareEmergency(boolean buzzerActive) {
        try {
            String deviceId = repository.getDeviceId();
            DatabaseReference deviceRef = FirebaseDatabase.getInstance()
                    .getReference(FirebaseConfig.getDevicePath(deviceId));

            long now = System.currentTimeMillis();
            Map<String, Object> testState = new HashMap<>();
            testState.put("deviceId", deviceId);
            testState.put("online", true);
            testState.put("lastSeen", now);
            testState.put("emergency", true);
            testState.put("buzzer", buzzerActive);
            testState.put("motion", true);
            testState.put("occupancy", "OCCUPIED");
            testState.put("status", "EMERGENCY");
            testState.put("alert_type", buzzerActive ? "NO_MOTION" : "EMERGENCY_BUTTON");

            deviceRef.updateChildren(testState)
                    .addOnSuccessListener(unused -> {
                        String msg = buzzerActive
                                ? "Firebase updated: Emergency with Buzzer ON (Local alarm test)."
                                : "Firebase updated: Emergency with Buzzer OFF (Emergency Notification test)!";
                        showToast(msg);
                    })
                    .addOnFailureListener(e -> showToast("Firebase update error: " + e.getMessage()));
        } catch (Exception e) {
            showToast("Error updating Firebase: " + e.getMessage());
        }
    }

    /** Shows dialog to configure Device ID. */
    private void showConfigDialog() {
        final EditText input = new EditText(this);
        input.setText(repository.getDeviceId());
        input.setSingleLine(true);

        new AlertDialog.Builder(this)
                .setTitle(R.string.demo_config_device_id)
                .setMessage("Enter the device ID your physical hardware publishes to (default: washroom_01):")
                .setView(input)
                .setPositiveButton(R.string.action_ok, (dialog, which) -> {
                    String newId = input.getText().toString().trim();
                    if (!newId.isEmpty()) {
                        repository.setDeviceId(newId);
                        updateDeviceIdDescription();
                        showToast("Device ID set to: " + newId);
                    }
                })
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }

    /** Creates a local simulated alert. */
    private void triggerDemoAlert(final AlertType type) {
        final AlertRecord record = AlertRecord.create(type, true);

        repository.saveAlert(record, new RepositoryCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                onAlertCreated(record);
            }

            @Override
            public void onError(Exception error) {
                onAlertCreated(record);
            }
        });
    }

    /** Called when the demo alert was stored. */
    private void onAlertCreated(AlertRecord record) {
        showToast(getString(R.string.demo_triggered));
        NotificationHelper.showAlertNotification(this, record.getType(), record.getTimestampMillis(), true);
        startActivity(AlertActivity.createIntent(this, record));
    }

    /** Switches the simulated connection between ONLINE and OFFLINE. */
    private void toggleConnection() {
        DeviceStatus current = repository.getConnectionStatus();
        DeviceStatus next = (current == DeviceStatus.OFFLINE) ? DeviceStatus.ONLINE : DeviceStatus.OFFLINE;

        repository.setDeviceStatus(next, new RepositoryCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                updateConnectionButton();
            }

            @Override
            public void onError(Exception error) {
                updateConnectionButton();
            }
        });
    }

    /** Shows the right label on the connection button. */
    private void updateConnectionButton() {
        DeviceStatus current = repository.getConnectionStatus();
        boolean offline = current == DeviceStatus.OFFLINE;

        connectionButton.setText(offline ? R.string.demo_set_online : R.string.demo_set_offline);
        connectionButton.setIconResource(offline ? R.drawable.ic_online : R.drawable.ic_offline);

        int strongColor = ContextCompat.getColor(this, current.getColorResId());
        connectionButton.setIconTint(ColorStateList.valueOf(strongColor));
        connectionButton.setTextColor(strongColor);
    }
}

package com.example.smartwashroom;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.example.smartwashroom.data.FirebaseRepository;
import com.example.smartwashroom.data.source.HardwareStateCallback;
import com.example.smartwashroom.model.AlertRecord;
import com.example.smartwashroom.model.AlertType;
import com.example.smartwashroom.model.DeviceStatus;
import com.example.smartwashroom.model.HardwareDeviceState;
import com.example.smartwashroom.ui.AlertActivity;
import com.example.smartwashroom.ui.AlertHistoryActivity;
import com.example.smartwashroom.ui.BaseActivity;
import com.example.smartwashroom.ui.ContactsActivity;
import com.example.smartwashroom.ui.DemoModeActivity;
import com.example.smartwashroom.ui.NearbyHospitalsActivity;
import com.example.smartwashroom.util.DateTimeUtils;

/**
 * HOME DASHBOARD.
 *
 * <p>Displays live hardware status from Firebase in real time, including device connection,
 * heartbeat, buzzer status (Ringing vs Not Ringing), occupancy/motion, current system state
 * (Normal, Warning, Emergency), and latest emergency alerts.</p>
 */
public class MainActivity extends BaseActivity {

    private FirebaseRepository repository;

    private TextView connectionStatusText;
    private TextView connectionDescriptionText;
    private TextView connectionUpdatedText;
    private ImageView connectionIcon;
    private View connectionIconBackground;

    private TextView currentStatusText;
    private TextView currentStatusDescriptionText;
    private ImageView currentStatusIcon;
    private View currentStatusIconBackground;

    // Telemetry views
    private TextView buzzerStatusText;
    private TextView motionStatusText;
    private TextView systemStateText;
    private TextView firebaseBadgeText;
    private TextView firebaseNoteText;

    private View latestAlertContent;
    private TextView latestAlertTypeText;
    private TextView latestAlertTimeText;
    private TextView latestAlertBadgeText;
    private ImageView latestAlertIcon;
    private TextView latestAlertEmptyText;
    private View latestAlertCard;

    @Override
    protected int getLayoutResourceId() {
        return R.layout.activity_main;
    }

    @Override
    protected String getToolbarTitle() {
        return getString(R.string.app_name);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        repository = FirebaseRepository.getInstance(this);

        bindViews();
        setupClickListeners();

        requestNotificationPermissionIfNeeded();
        setupRealtimeObservers();
        refreshDashboard();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshDashboard();
    }

    /** Finds all the views of the screen. */
    private void bindViews() {
        connectionStatusText = findViewById(R.id.tv_connection_status);
        connectionDescriptionText = findViewById(R.id.tv_connection_desc);
        connectionUpdatedText = findViewById(R.id.tv_connection_updated);
        connectionIcon = findViewById(R.id.iv_connection_icon);
        connectionIconBackground = findViewById(R.id.connection_icon_background);

        currentStatusText = findViewById(R.id.tv_current_status_label);
        currentStatusDescriptionText = findViewById(R.id.tv_current_status_desc);
        currentStatusIcon = findViewById(R.id.iv_current_status);
        currentStatusIconBackground = findViewById(R.id.current_status_icon_background);

        buzzerStatusText = findViewById(R.id.tv_buzzer_status);
        motionStatusText = findViewById(R.id.tv_motion_status);
        systemStateText = findViewById(R.id.tv_system_state);
        firebaseBadgeText = findViewById(R.id.tv_firebase_badge);
        firebaseNoteText = findViewById(R.id.tv_firebase_note);

        latestAlertContent = findViewById(R.id.latest_alert_content);
        latestAlertTypeText = findViewById(R.id.tv_latest_alert_type);
        latestAlertTimeText = findViewById(R.id.tv_latest_alert_time);
        latestAlertBadgeText = findViewById(R.id.tv_latest_alert_badge);
        latestAlertIcon = findViewById(R.id.iv_latest_alert);
        latestAlertEmptyText = findViewById(R.id.tv_latest_alert_empty);
        latestAlertCard = findViewById(R.id.latest_alert_card);
    }

    /** Subscribes to real-time hardware status changes from Firebase. */
    private void setupRealtimeObservers() {
        repository.observeHardwareState(new HardwareStateCallback() {
            @Override
            public void onHardwareStateChanged(HardwareDeviceState state) {
                runOnUiThread(() -> {
                    updateHardwareDashboard(state);
                    updateLatestAlertCard();
                });
            }

            @Override
            public void onError(Exception error) {
                runOnUiThread(() -> {
                    if (firebaseBadgeText != null) {
                        firebaseBadgeText.setText("FIREBASE: ERROR");
                        firebaseBadgeText.setBackgroundTintList(ColorStateList.valueOf(
                                ContextCompat.getColor(MainActivity.this, R.color.alert_danger)));
                    }
                    if (firebaseNoteText != null) {
                        firebaseNoteText.setText("Error connecting to Firebase: " + (error != null ? error.getMessage() : "Unknown"));
                    }
                });
            }
        });
    }

    /** Connects navigation buttons. */
    private void setupClickListeners() {
        findViewById(R.id.card_contacts).setOnClickListener(view ->
                startActivity(new Intent(this, ContactsActivity.class)));

        findViewById(R.id.card_history).setOnClickListener(view ->
                startActivity(new Intent(this, AlertHistoryActivity.class)));

        findViewById(R.id.card_hospitals).setOnClickListener(view ->
                startActivity(new Intent(this, NearbyHospitalsActivity.class)));

        findViewById(R.id.card_demo).setOnClickListener(view ->
                startActivity(new Intent(this, DemoModeActivity.class)));

        latestAlertCard.setOnClickListener(view -> {
            AlertRecord latest = repository == null ? null : getLatestAlert();
            if (latest == null) {
                startActivity(new Intent(this, AlertHistoryActivity.class));
            } else {
                startActivity(AlertActivity.createIntent(this, latest));
            }
        });
    }

    /** Reads the latest alert once. */
    private AlertRecord getLatestAlert() {
        final AlertRecord[] result = new AlertRecord[1];
        repository.loadLatestAlert(record -> result[0] = record);
        return result[0];
    }

    /** Refreshes every part of the dashboard. */
    private void refreshDashboard() {
        HardwareDeviceState state = repository.getHardwareDeviceState();
        updateHardwareDashboard(state);
        updateLatestAlertCard();
    }

    /** Updates the telemetry cards with live hardware state. */
    private void updateHardwareDashboard(HardwareDeviceState state) {
        if (state == null) {
            state = HardwareDeviceState.createDefault(repository.getDeviceId());
        }

        DeviceStatus connectionStatus = state.isOnline() ? DeviceStatus.ONLINE : DeviceStatus.OFFLINE;
        updateConnectionCard(connectionStatus, state.getLastSeen());
        updateStatusCard(state.toDeviceStatus(), state);
        updateTelemetryRows(state);
        updateFirebaseBadge();
    }

    /** Fills the "Device connection" card. */
    private void updateConnectionCard(DeviceStatus connectionStatus, long lastUpdatedAt) {
        int colorResId = connectionStatus.getColorResId();
        int lightColorResId = connectionStatus.getLightColorResId();
        int strongColor = ContextCompat.getColor(this, colorResId);
        int softColor = ContextCompat.getColor(this, lightColorResId);

        connectionStatusText.setText(connectionStatus.getLabelResId());
        connectionStatusText.setTextColor(strongColor);
        connectionStatusText.setBackgroundTintList(ColorStateList.valueOf(softColor));

        connectionIcon.setImageResource(connectionStatus.getIconResId());
        connectionIcon.setColorFilter(strongColor);
        connectionIconBackground.setBackgroundTintList(ColorStateList.valueOf(softColor));

        connectionDescriptionText.setText(connectionStatus == DeviceStatus.OFFLINE
                ? R.string.home_connection_offline
                : R.string.home_connection_online);

        if (lastUpdatedAt > 0) {
            connectionUpdatedText.setText(getString(R.string.home_heartbeat,
                    DateTimeUtils.formatDateTime(lastUpdatedAt)));
            connectionUpdatedText.setVisibility(View.VISIBLE);
        } else {
            connectionUpdatedText.setVisibility(View.GONE);
        }
    }

    /** Fills the telemetry section (Buzzer, Occupancy, System State). */
    private void updateTelemetryRows(HardwareDeviceState state) {
        // 1. Buzzer Status (Ringing vs Silent)
        if (state.isBuzzerActive()) {
            buzzerStatusText.setText(R.string.hardware_buzzer_ringing);
            buzzerStatusText.setTextColor(ContextCompat.getColor(this, R.color.alert_danger));
            buzzerStatusText.setBackgroundTintList(ColorStateList.valueOf(
                    ContextCompat.getColor(this, R.color.alert_danger_light)));
        } else {
            buzzerStatusText.setText(R.string.hardware_buzzer_silent);
            buzzerStatusText.setTextColor(ContextCompat.getColor(this, R.color.status_online));
            buzzerStatusText.setBackgroundTintList(ColorStateList.valueOf(
                    ContextCompat.getColor(this, R.color.status_online_light)));
        }

        // 2. Motion / Occupancy Status
        if (state.isMotionDetected()) {
            motionStatusText.setText(R.string.hardware_occupancy_motion);
            motionStatusText.setTextColor(ContextCompat.getColor(this, R.color.brand_primary));
            motionStatusText.setBackgroundTintList(ColorStateList.valueOf(
                    ContextCompat.getColor(this, R.color.brand_secondary)));
        } else {
            motionStatusText.setText(R.string.hardware_occupancy_vacant);
            motionStatusText.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
            motionStatusText.setBackgroundTintList(ColorStateList.valueOf(
                    ContextCompat.getColor(this, R.color.surface_page)));
        }

        // 3. System Status: Normal, Warning, Emergency
        String sysState = state.getSystemStatus();
        if (HardwareDeviceState.STATUS_EMERGENCY.equalsIgnoreCase(sysState)) {
            systemStateText.setText(R.string.hardware_system_emergency);
            systemStateText.setTextColor(ContextCompat.getColor(this, R.color.alert_danger));
            systemStateText.setBackgroundTintList(ColorStateList.valueOf(
                    ContextCompat.getColor(this, R.color.alert_danger_light)));
        } else if (HardwareDeviceState.STATUS_WARNING.equalsIgnoreCase(sysState)) {
            systemStateText.setText(R.string.hardware_system_warning);
            systemStateText.setTextColor(ContextCompat.getColor(this, R.color.alert_warning));
            systemStateText.setBackgroundTintList(ColorStateList.valueOf(
                    ContextCompat.getColor(this, R.color.alert_warning_light)));
        } else {
            systemStateText.setText(R.string.hardware_system_normal);
            systemStateText.setTextColor(ContextCompat.getColor(this, R.color.status_online));
            systemStateText.setBackgroundTintList(ColorStateList.valueOf(
                    ContextCompat.getColor(this, R.color.status_online_light)));
        }
    }

    /** Fills the "Current status" card. */
    private void updateStatusCard(DeviceStatus status, HardwareDeviceState state) {
        int strongColor = ContextCompat.getColor(this, status.getColorResId());
        int softColor = ContextCompat.getColor(this, status.getLightColorResId());

        currentStatusText.setText(status.getLabelResId());
        currentStatusIcon.setImageResource(status.getIconResId());
        currentStatusIcon.setColorFilter(strongColor);
        currentStatusIconBackground.setBackgroundTintList(ColorStateList.valueOf(softColor));

        if (state.isEmergencyWithBuzzerOff()) {
            currentStatusDescriptionText.setText("EMERGENCY DETECTED (Buzzer OFF): Family members notified!");
        } else {
            switch (status) {
                case OFFLINE:
                    currentStatusDescriptionText.setText(R.string.home_status_offline);
                    break;
                case NO_MOTION_ALERT:
                    currentStatusDescriptionText.setText(R.string.home_status_no_motion);
                    break;
                case EMERGENCY_ALERT:
                    currentStatusDescriptionText.setText(R.string.home_status_emergency);
                    break;
                case ONLINE:
                default:
                    currentStatusDescriptionText.setText(R.string.home_status_online);
                    break;
            }
        }
    }

    /** Fills the "Latest alert" card. */
    private void updateLatestAlertCard() {
        repository.loadLatestAlert(record -> {
            if (record == null) {
                latestAlertContent.setVisibility(View.GONE);
                latestAlertEmptyText.setVisibility(View.VISIBLE);
                latestAlertEmptyText.setText(R.string.home_no_alerts_hint);
                return;
            }

            latestAlertContent.setVisibility(View.VISIBLE);
            latestAlertEmptyText.setVisibility(View.GONE);

            AlertType type = record.getType();
            if (type == null) type = AlertType.EMERGENCY_BUTTON;

            latestAlertTypeText.setText(type.getTitleResId());
            latestAlertTimeText.setText(DateTimeUtils.formatDateTime(record.getTimestampMillis()));

            int strongColor = ContextCompat.getColor(this, type.getColorResId());
            latestAlertIcon.setImageResource(type.getIconResId());
            latestAlertIcon.setColorFilter(strongColor);

            if (record.isDemo()) {
                latestAlertBadgeText.setText(R.string.alert_demo_badge);
                latestAlertBadgeText.setBackgroundTintList(ColorStateList.valueOf(
                        ContextCompat.getColor(this, R.color.alert_warning_light)));
                latestAlertBadgeText.setTextColor(ContextCompat.getColor(this, R.color.alert_warning));
                latestAlertBadgeText.setVisibility(View.VISIBLE);
            } else {
                latestAlertBadgeText.setText(R.string.history_item_live);
                latestAlertBadgeText.setBackgroundTintList(ColorStateList.valueOf(
                        ContextCompat.getColor(this, R.color.alert_danger_light)));
                latestAlertBadgeText.setTextColor(ContextCompat.getColor(this, R.color.alert_danger));
                latestAlertBadgeText.setVisibility(View.VISIBLE);
            }
        });
    }

    private void updateFirebaseBadge() {
        if (firebaseBadgeText == null || firebaseNoteText == null) return;

        if (repository.isFirebaseActive()) {
            firebaseBadgeText.setText(R.string.firebase_active_badge);
            firebaseBadgeText.setBackgroundTintList(ColorStateList.valueOf(
                    ContextCompat.getColor(this, R.color.status_online)));
            firebaseBadgeText.setTextColor(ContextCompat.getColor(this, R.color.text_on_dark));
            firebaseNoteText.setText(R.string.firebase_active_note);
        } else {
            firebaseBadgeText.setText(R.string.firebase_pending_badge);
            firebaseBadgeText.setBackgroundTintList(ColorStateList.valueOf(
                    ContextCompat.getColor(this, R.color.brand_secondary)));
            firebaseNoteText.setText(R.string.firebase_pending_note);
        }
    }
}

package com.example.smartwashroom.ui;

import android.app.AlertDialog;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.example.smartwashroom.R;
import com.example.smartwashroom.data.FirebaseRepository;
import com.example.smartwashroom.data.source.RepositoryCallback;
import com.example.smartwashroom.model.AlertRecord;
import com.example.smartwashroom.util.DateTimeUtils;

import java.util.List;

/**
 * ALERT HISTORY screen.
 *
 * <p>Shows every alert from Firebase and local demo history (newest first) with its
 * type, date, time and a "DEMO" or "LIVE" badge so simulated alerts are never
 * mistaken for real emergencies.</p>
 */
public class AlertHistoryActivity extends BaseActivity {

    private FirebaseRepository repository;

    private LinearLayout historyContainer;
    private View emptyPanel;
    private View clearButton;

    @Override
    protected int getLayoutResourceId() {
        return R.layout.activity_alert_history;
    }

    @Override
    protected String getToolbarTitle() {
        return getString(R.string.history_title);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        repository = FirebaseRepository.getInstance(this);

        historyContainer = findViewById(R.id.alert_history_container);
        emptyPanel = findViewById(R.id.empty_history_panel);
        clearButton = findViewById(R.id.btn_clear_history);

        clearButton.setOnClickListener(view -> confirmClearHistory());
    }

    @Override
    protected void onResume() {
        super.onResume();
        showHistory();
    }

    /** Builds the list of stored alerts. */
    private void showHistory() {
        repository.loadAlertHistory(new RepositoryCallback<List<AlertRecord>>() {
            @Override
            public void onSuccess(List<AlertRecord> records) {
                renderHistory(records);
            }

            @Override
            public void onError(Exception error) {
                renderHistory(null);
            }
        });
    }

    /** Puts the given alerts into the list. */
    private void renderHistory(List<AlertRecord> records) {
        historyContainer.removeAllViews();

        boolean empty = (records == null || records.isEmpty());
        emptyPanel.setVisibility(empty ? View.VISIBLE : View.GONE);
        clearButton.setVisibility(empty ? View.GONE : View.VISIBLE);

        if (empty) {
            return;
        }

        LayoutInflater inflater = LayoutInflater.from(this);
        for (AlertRecord record : records) {
            View row = inflater.inflate(R.layout.item_alert_history, historyContainer, false);

            int strongColor = ContextCompat.getColor(this, record.getType().getColorResId());
            int softColor = ContextCompat.getColor(this, record.getType().getLightColorResId());

            ImageView icon = row.findViewById(R.id.iv_history_icon);
            icon.setImageResource(record.getType().getIconResId());
            icon.setColorFilter(strongColor);

            View iconBackground = row.findViewById(R.id.history_icon_background);
            iconBackground.setBackgroundTintList(ColorStateList.valueOf(softColor));

            TextView typeText = row.findViewById(R.id.tv_history_type);
            typeText.setText(record.getType().getTitleResId());

            TextView dateTimeText = row.findViewById(R.id.tv_history_datetime);
            dateTimeText.setText(getString(R.string.history_item_datetime,
                    DateTimeUtils.formatDate(record.getTimestampMillis()),
                    DateTimeUtils.formatTime(record.getTimestampMillis())));

            TextView badge = row.findViewById(R.id.tv_history_demo_badge);
            if (record.isDemo()) {
                badge.setText(R.string.history_item_demo);
                badge.setBackgroundTintList(ColorStateList.valueOf(
                        ContextCompat.getColor(this, R.color.alert_warning_light)));
                badge.setTextColor(ContextCompat.getColor(this, R.color.alert_warning));
            } else {
                badge.setText(R.string.history_item_live);
                badge.setBackgroundTintList(ColorStateList.valueOf(
                        ContextCompat.getColor(this, R.color.alert_danger_light)));
                badge.setTextColor(ContextCompat.getColor(this, R.color.alert_danger));
            }
            badge.setVisibility(View.VISIBLE);

            historyContainer.addView(row);
        }
    }

    /** Asks before deleting the whole history. */
    private void confirmClearHistory() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.history_clear_title)
                .setMessage(R.string.history_clear_message)
                .setPositiveButton(R.string.action_clear_history, (dialog, which) -> {
                    repository.clearAlertHistory(new RepositoryCallback<Void>() {
                        @Override
                        public void onSuccess(Void result) {
                            showToast(getString(R.string.history_cleared));
                            showHistory();
                        }

                        @Override
                        public void onError(Exception error) {
                            showToast(getString(R.string.history_cleared));
                            showHistory();
                        }
                    });
                })
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }
}

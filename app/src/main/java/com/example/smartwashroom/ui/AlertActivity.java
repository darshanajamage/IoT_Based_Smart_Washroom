package com.example.smartwashroom.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.example.smartwashroom.R;
import com.example.smartwashroom.data.ContactRepository;
import com.example.smartwashroom.data.FirebaseRepository;
import com.example.smartwashroom.data.source.RepositoryCallback;
import com.example.smartwashroom.model.AlertRecord;
import com.example.smartwashroom.model.AlertType;
import com.example.smartwashroom.model.DeviceStatus;
import com.example.smartwashroom.model.FamilyContact;
import com.example.smartwashroom.util.DateTimeUtils;
import com.example.smartwashroom.util.DialerHelper;
import com.google.android.material.card.MaterialCardView;

import java.util.List;

/**
 * ALERT WARNING SCREEN.
 *
 * <p>The screen is used for both alert types. The colours change with the type:
 * amber for "no motion detected" and red for "emergency button pressed".</p>
 *
 * <p>From here the user can call one of the saved family members, open the
 * hospital screen or mark the alert as handled.</p>
 */
public class AlertActivity extends BaseActivity {

    /** Extra: the name of the {@link AlertType} constant. */
    public static final String EXTRA_ALERT_TYPE = "extra_alert_type";
    /** Extra: the time the alert was raised (in milliseconds). */
    public static final String EXTRA_ALERT_TIMESTAMP = "extra_alert_timestamp";
    /** Extra: true when the alert was created in Demo Mode. */
    public static final String EXTRA_IS_DEMO = "extra_is_demo";

    private FirebaseRepository repository;
    private ContactRepository contactRepository;

    private AlertType alertType;
    private long alertTimestamp;
    private boolean isDemo;

    /** Builds the intent that opens this screen for the given alert. */
    public static Intent createIntent(Context context, AlertRecord record) {
        Intent intent = new Intent(context, AlertActivity.class);
        intent.putExtra(EXTRA_ALERT_TYPE, record.getType().name());
        intent.putExtra(EXTRA_ALERT_TIMESTAMP, record.getTimestampMillis());
        intent.putExtra(EXTRA_IS_DEMO, record.isDemo());
        return intent;
    }

    @Override
    protected int getLayoutResourceId() {
        return R.layout.activity_alert;
    }

    @Override
    protected String getToolbarTitle() {
        return getString(R.string.alert_screen_title);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        repository = FirebaseRepository.getInstance(this);
        contactRepository = ContactRepository.getInstance(this);

        readExtras();
        if (alertType == null) {
            // Nothing to show - close the screen again.
            finish();
            return;
        }

        applyAlertStyle();
        showAlertDetails();
        setupClickListeners();
    }

    /** Reads the information about the alert from the intent. */
    private void readExtras() {
        alertType = AlertType.fromName(getIntent().getStringExtra(EXTRA_ALERT_TYPE));
        alertTimestamp = getIntent().getLongExtra(EXTRA_ALERT_TIMESTAMP, System.currentTimeMillis());
        isDemo = getIntent().getBooleanExtra(EXTRA_IS_DEMO, false);
    }

    /** Colours the screen depending on the type of the alert. */
    private void applyAlertStyle() {
        int strongColor = ContextCompat.getColor(this, alertType.getColorResId());
        int softColor = ContextCompat.getColor(this, alertType.getLightColorResId());

        View header = findViewById(R.id.header_container);
        header.setBackgroundTintList(ColorStateList.valueOf(strongColor));

        View bannerCard = findViewById(R.id.alert_banner_card);
        if (bannerCard instanceof MaterialCardView) {
            MaterialCardView card = (MaterialCardView) bannerCard;
            card.setCardBackgroundColor(softColor);
            card.setStrokeColor(strongColor);
        }

        View iconBackground = findViewById(R.id.alert_icon_background);
        iconBackground.setBackgroundTintList(ColorStateList.valueOf(strongColor));

        ImageView icon = findViewById(R.id.iv_alert_icon);
        icon.setImageResource(alertType.getIconResId());
        icon.setColorFilter(ContextCompat.getColor(this, R.color.text_on_dark));

        TextView typeText = findViewById(R.id.tv_alert_type);
        typeText.setText(alertType.getTitleResId());

        TextView explainerText = findViewById(R.id.tv_alert_explainer);
        explainerText.setText(alertType.getExplainerResId());

        TextView badge = findViewById(R.id.tv_alert_demo_badge);
        badge.setVisibility(isDemo ? View.VISIBLE : View.GONE);
    }

    /** Fills the "Alert details" card. */
    private void showAlertDetails() {
        TextView typeText = findViewById(R.id.tv_detail_type);
        typeText.setText(alertType.getTitleResId());

        TextView dateText = findViewById(R.id.tv_detail_date);
        dateText.setText(DateTimeUtils.formatDate(alertTimestamp));

        TextView timeText = findViewById(R.id.tv_detail_time);
        timeText.setText(DateTimeUtils.formatTime(alertTimestamp));

        TextView sourceText = findViewById(R.id.tv_detail_source);
        sourceText.setText(isDemo ? R.string.alert_source_demo : R.string.alert_source_device);

        // The device status is always shown separately from the alert itself.
        TextView statusText = findViewById(R.id.tv_detail_device_status);
        DeviceStatus currentStatus = repository.getDisplayStatus();
        statusText.setText(currentStatus.getLabelResId());
        statusText.setTextColor(ContextCompat.getColor(this, currentStatus.getColorResId()));
    }

    /** Connects the three buttons of the screen. */
    private void setupClickListeners() {
        findViewById(R.id.btn_call_family).setOnClickListener(view -> showContactChooser());
        findViewById(R.id.btn_open_hospitals).setOnClickListener(view ->
                startActivity(new Intent(this, NearbyHospitalsActivity.class)));
        findViewById(R.id.btn_acknowledge).setOnClickListener(view -> acknowledgeAlert());
    }

    /**
     * Lets the user choose which family member should be called. If no contact is
     * saved yet, the user is taken to the contacts screen.
     */
    private void showContactChooser() {
        final List<FamilyContact> contacts = contactRepository.getContacts();
        if (contacts.isEmpty()) {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.alert_no_contacts_title)
                    .setMessage(R.string.alert_no_contacts_message)
                    .setPositiveButton(R.string.action_add_contact, (dialog, which) ->
                            startActivity(new Intent(this, ContactsActivity.class)))
                    .setNegativeButton(R.string.action_cancel, null)
                    .show();
            return;
        }

        String[] names = new String[contacts.size()];
        for (int i = 0; i < contacts.size(); i++) {
            names[i] = contacts.get(i).getName() + "  (" + contacts.get(i).getPhoneNumber() + ")";
        }

        new AlertDialog.Builder(this)
                .setTitle(R.string.alert_choose_contact_title)
                .setItems(names, (dialog, which) -> confirmCall(contacts.get(which)))
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }

    /** Asks for confirmation and then opens the dialler. */
    private void confirmCall(FamilyContact contact) {
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.contact_call_title, contact.getName()))
                .setMessage(getString(R.string.contact_call_message, contact.getPhoneNumber()))
                .setPositiveButton(R.string.action_call, (dialog, which) ->
                        DialerHelper.dial(this, contact.getPhoneNumber()))
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }

    /** Clears the active alert and closes the screen. */
    private void acknowledgeAlert() {
        repository.acknowledgeAlert(new RepositoryCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                showToast(getString(R.string.alert_acknowledged));
                finish();
            }

            @Override
            public void onError(Exception error) {
                showToast(getString(R.string.alert_acknowledged));
                finish();
            }
        });
    }
}

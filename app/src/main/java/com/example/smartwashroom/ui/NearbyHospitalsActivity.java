package com.example.smartwashroom.ui;

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.widget.TextView;

import com.example.smartwashroom.R;
import com.google.android.material.textfield.TextInputEditText;

/**
 * NEARBY HOSPITALS screen.
 *
 * <p>The screen opens Google Maps with a search for hospitals around the user.
 * When Google Maps is not installed the user is told about it and offered the
 * web version of Google Maps in the browser instead.</p>
 */
public class NearbyHospitalsActivity extends BaseActivity {

    /** Package name of the Google Maps app. */
    private static final String GOOGLE_MAPS_PACKAGE = "com.google.android.apps.maps";

    /** Web version of the Google Maps search, used when the app is missing. */
    private static final String GOOGLE_MAPS_WEB_SEARCH =
            "https://www.google.com/maps/search/?api=1&query=";

    private TextInputEditText searchInput;
    private TextView mapsStatusText;

    @Override
    protected int getLayoutResourceId() {
        return R.layout.activity_nearby_hospitals;
    }

    @Override
    protected String getToolbarTitle() {
        return getString(R.string.hospitals_title);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        searchInput = findViewById(R.id.et_search);
        mapsStatusText = findViewById(R.id.tv_maps_status);

        findViewById(R.id.btn_open_nearby_hospitals).setOnClickListener(view ->
                openInGoogleMaps(buildQuery(null)));
        findViewById(R.id.btn_search_hospitals).setOnClickListener(view -> {
            String typed = searchInput.getText() == null ? "" : searchInput.getText().toString().trim();
            openInGoogleMaps(buildQuery(typed));
        });
        findViewById(R.id.btn_open_any_map).setOnClickListener(view -> openInAnyMapApp());

        updateMapsStatus();
    }

    /**
     * Builds the search text, for example "hospitals near me" or
     * "hospitals near Pune".
     */
    private String buildQuery(String typedSearch) {
        if (typedSearch == null || typedSearch.isEmpty()) {
            return getString(R.string.hospitals_search_query_empty);
        }
        return getString(R.string.hospitals_search_query_named, typedSearch);
    }

    /** Shows whether Google Maps is available on this device. */
    private void updateMapsStatus() {
        if (isGoogleMapsAvailable()) {
            mapsStatusText.setText(R.string.hospitals_intro);
        } else {
            mapsStatusText.setText(R.string.hospitals_maps_missing_message);
        }
    }

    /**
     * @return true when the Google Maps app is installed <b>and</b> able to open a
     *         map search. Google Maps can be present but switched off, and in that
     *         case no activity can be started for it.
     */
    private boolean isGoogleMapsAvailable() {
        PackageManager packageManager = getPackageManager();
        try {
            packageManager.getPackageInfo(GOOGLE_MAPS_PACKAGE, 0);
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
        return resolveMapsActivity(packageManager) != null;
    }

    /**
     * @return the activity that would open a map search in Google Maps, or null
     *         when there is none.
     */
    private ResolveInfo resolveMapsActivity(PackageManager packageManager) {
        Intent probe = new Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=" + Uri.encode("hospital")));
        probe.setPackage(GOOGLE_MAPS_PACKAGE);
        return packageManager.resolveActivity(probe, 0);
    }

    /**
     * Opens Google Maps with the given search.
     *
     * <p>When Google Maps is not installed a dialog is shown and the user can
     * open the same search in a browser instead.</p>
     */
    private void openInGoogleMaps(String query) {
        if (!isGoogleMapsAvailable()) {
            showMapsMissingDialog(query);
            return;
        }

        Uri uri = Uri.parse("geo:0,0?q=" + Uri.encode(query));
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        intent.setPackage(GOOGLE_MAPS_PACKAGE);

        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            showMapsMissingDialog(query);
        }
    }

    /** Opens the search with any map app that is installed. */
    private void openInAnyMapApp() {
        String query = buildQuery(searchInput.getText() == null ? "" : searchInput.getText().toString().trim());
        Uri uri = Uri.parse("geo:0,0?q=" + Uri.encode(query));
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);

        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            showToast(getString(R.string.hospitals_no_map_app));
        }
    }

    /**
     * Tells the user that Google Maps is missing and offers the web version as an
     * alternative.
     */
    private void showMapsMissingDialog(final String query) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.hospitals_maps_missing_title)
                .setMessage(R.string.hospitals_maps_missing_message)
                .setPositiveButton(R.string.action_open_browser, (dialog, which) -> openInBrowser(query))
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }

    /** Opens the Google Maps web page with the hospital search. */
    private void openInBrowser(String query) {
        Intent intent = new Intent(Intent.ACTION_VIEW,
                Uri.parse(GOOGLE_MAPS_WEB_SEARCH + Uri.encode(query)));
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            showToast(getString(R.string.hospitals_no_map_app));
        }
    }
}

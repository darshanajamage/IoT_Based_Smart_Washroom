package com.example.smartwashroom.ui;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.smartwashroom.R;
import com.google.android.material.appbar.MaterialToolbar;

/**
 * Common base class for every screen of the app.
 *
 * <p>It keeps the app bar (with the back arrow), the handling of the status bar
 * and the navigation bar insets and the notification permission in one place, so
 * that every screen looks and behaves the same way.</p>
 *
 * <p>Every layout used by a subclass must contain these three views:</p>
 * <ul>
 *     <li>{@code @id/root} - the outermost layout</li>
 *     <li>{@code @id/header_container} - the coloured app bar area</li>
 *     <li>{@code @id/content} - the scrollable content area</li>
 * </ul>
 */
public abstract class BaseActivity extends AppCompatActivity {

    private ActivityResultLauncher<String> notificationPermissionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(getLayoutResourceId());

        applySystemBarInsets();
        setupToolbar();

        notificationPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                granted -> {
                    if (!granted) {
                        showToast(getString(R.string.notification_permission_denied));
                    }
                });
    }

    /** @return the XML layout that this screen uses. */
    protected abstract int getLayoutResourceId();

    /** @return the title that is shown in the app bar. */
    protected abstract String getToolbarTitle();

    /**
     * Keeps the coloured app bar visible behind the status bar and makes sure the
     * content is not hidden behind the navigation bar.
     */
    private void applySystemBarInsets() {
        View root = findViewById(R.id.root);
        View header = findViewById(R.id.header_container);
        View content = findViewById(R.id.content);
        if (root == null) {
            return;
        }
        ViewCompat.setOnApplyWindowInsetsListener(root, (view, windowInsets) -> {
            Insets bars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            if (header != null) {
                header.setPadding(header.getPaddingLeft(), bars.top,
                        header.getPaddingRight(), header.getPaddingBottom());
            }
            if (content != null) {
                content.setPadding(content.getPaddingLeft() + bars.left,
                        content.getPaddingTop(),
                        content.getPaddingRight() + bars.right,
                        content.getPaddingBottom() + bars.bottom);
            }
            return windowInsets;
        });
    }

    /** Sets up the app bar with a working back arrow. */
    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar == null) {
            return;
        }
        toolbar.setTitle(getToolbarTitle());
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back);
        toolbar.setNavigationIconTint(ContextCompat.getColor(this, R.color.text_on_dark));
        toolbar.setNavigationOnClickListener(view -> getOnBackPressedDispatcher().onBackPressed());
    }

    /**
     * Asks the user for the "notifications" permission (Android 13 and newer).
     * On older Android versions the permission is granted automatically.
     */
    protected void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return;
        }
        boolean alreadyAllowed = ContextCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
        if (alreadyAllowed) {
            return;
        }

        // If the user refused before, Android asks us to explain why we need it.
        if (shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
            new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle(R.string.notification_channel_name)
                    .setMessage(R.string.notification_permission_rationale)
                    .setPositiveButton(R.string.action_ok, (dialog, which) ->
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS))
                    .setNegativeButton(R.string.action_cancel, null)
                    .show();
            return;
        }

        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
    }

    /** Shows a short message at the bottom of the screen. */
    protected void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}

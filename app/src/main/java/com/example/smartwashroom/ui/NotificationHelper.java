package com.example.smartwashroom.ui;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.example.smartwashroom.R;
import com.example.smartwashroom.model.AlertType;

/**
 * Creates and displays safety alert notifications.
 *
 * <p>Configures a high-priority notification channel with alarm attributes and vibration,
 * ensuring immediate delivery when emergency events are received from Firebase or Demo Mode.</p>
 */
public final class NotificationHelper {

    /** Channel ID for emergency safety alerts. */
    public static final String CHANNEL_ID = "safety_alerts";

    private NotificationHelper() {
        // Utility class
    }

    /** Creates the notification channel with high priority, sound, and vibration. */
    public static void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (manager == null) {
            return;
        }

        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_HIGH);
        channel.setDescription(context.getString(R.string.notification_channel_description));
        channel.enableLights(true);
        channel.enableVibration(true);
        channel.setVibrationPattern(new long[]{0, 500, 250, 500, 250, 500});

        Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_ALARM)
                .build();
        channel.setSound(alarmSound, audioAttributes);

        manager.createNotificationChannel(channel);
    }

    /**
     * Shows a notification for the given alert. Tapping it opens the alert screen.
     *
     * @param demo true when the alert was simulated in Demo Mode.
     */
    public static void showAlertNotification(Context context, AlertType type, long timestampMillis, boolean demo) {
        if (type == null) {
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        createNotificationChannel(context);

        Intent intent = new Intent(context, AlertActivity.class);
        intent.putExtra(AlertActivity.EXTRA_ALERT_TYPE, type.name());
        intent.putExtra(AlertActivity.EXTRA_ALERT_TIMESTAMP, timestampMillis);
        intent.putExtra(AlertActivity.EXTRA_IS_DEMO, demo);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                type.ordinal() + (demo ? 1000 : 0),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        int titleResId = (type == AlertType.EMERGENCY_BUTTON)
                ? R.string.notification_title_emergency
                : R.string.notification_title_no_motion;

        String contentText = context.getString(R.string.notification_body);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(context.getString(titleResId))
                .setContentText(contentText)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(contentText))
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setVibrate(new long[]{0, 500, 250, 500, 250, 500})
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        if (demo) {
            builder.setSubText(context.getString(R.string.alert_demo_badge));
        } else {
            builder.setSubText("SMART WASHROOM LIVE");
        }

        try {
            NotificationManagerCompat.from(context).notify(type.ordinal() + 100, builder.build());
        } catch (SecurityException ignored) {
            // Permission revoked
        }
    }
}

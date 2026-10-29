package com.example.smartwashroom;

import android.app.Application;
import android.util.Log;

import com.example.smartwashroom.data.FirebaseConfig;
import com.example.smartwashroom.ui.NotificationHelper;
import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.FirebaseMessaging;

/**
 * Application class of the app.
 *
 * <p>Initializes Firebase components, creates the notification channels,
 * registers the device for FCM push alerts, and subscribes to the emergency topic.</p>
 */
public class SafetyApplication extends Application {

    private static final String TAG = "SafetyApplication";

    @Override
    public void onCreate() {
        super.onCreate();

        // 1. Initialize notification channels early
        NotificationHelper.createNotificationChannel(this);

        // 2. Initialize Firebase
        try {
            FirebaseApp.initializeApp(this);
            Log.i(TAG, "Firebase initialized successfully.");

            // 3. Subscribe to emergency alert topic for FCM
            FirebaseMessaging.getInstance().subscribeToTopic(FirebaseConfig.FCM_TOPIC_EMERGENCY)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Log.i(TAG, "Subscribed to FCM topic: " + FirebaseConfig.FCM_TOPIC_EMERGENCY);
                        } else {
                            Log.w(TAG, "Failed to subscribe to FCM topic", task.getException());
                        }
                    });

            // 4. Retrieve and register initial FCM token
            FirebaseMessaging.getInstance().getToken()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful() && task.getResult() != null) {
                            String token = task.getResult();
                            SafetyFirebaseMessagingService.registerTokenWithFirebase(token);
                        }
                    });

        } catch (Exception e) {
            Log.e(TAG, "Firebase initialization error", e);
        }
    }
}

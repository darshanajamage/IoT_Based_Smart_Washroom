package com.example.smartwashroom.data.source;

import com.example.smartwashroom.model.HardwareDeviceState;

/**
 * Real-time callback for receiving comprehensive live hardware state updates from Firebase.
 */
public interface HardwareStateCallback {

    /** Called whenever hardware data changes in Firebase. */
    void onHardwareStateChanged(HardwareDeviceState state);

    /** Called when an error occurs (such as permission denied or network failure). */
    void onError(Exception error);
}

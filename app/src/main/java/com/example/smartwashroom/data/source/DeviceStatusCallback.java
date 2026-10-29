package com.example.smartwashroom.data.source;

import com.example.smartwashroom.model.DeviceStatus;

/**
 * Callback used when the device status changes.
 *
 * <p>This is the hook that will later be driven by a Firebase Realtime Database
 * listener. With the local demo data source it is called straight away with the
 * status that is stored on the phone.</p>
 */
public interface DeviceStatusCallback {

    void onDeviceStatusChanged(DeviceStatus status);
}

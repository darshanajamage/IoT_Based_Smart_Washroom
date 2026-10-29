package com.example.smartwashroom.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Tests for the device status.
 *
 * <p>The most important rule of this test: a "no motion" alert is NOT the same
 * thing as an offline device. If that ever changes, the app would report a
 * safety alert as a connection problem, which would be dangerous.</p>
 */
public class DeviceStatusTest {

    @Test
    public void theFourStatesExist() {
        assertEquals(4, DeviceStatus.values().length);
        assertEquals("ONLINE", DeviceStatus.ONLINE.name());
        assertEquals("OFFLINE", DeviceStatus.OFFLINE.name());
        assertEquals("NO_MOTION_ALERT", DeviceStatus.NO_MOTION_ALERT.name());
        assertEquals("EMERGENCY_ALERT", DeviceStatus.EMERGENCY_ALERT.name());
    }

    @Test
    public void onlyTheTwoAlertStatesAreAlerts() {
        assertFalse(DeviceStatus.ONLINE.isAlert());
        assertFalse(DeviceStatus.OFFLINE.isAlert());
        assertTrue(DeviceStatus.NO_MOTION_ALERT.isAlert());
        assertTrue(DeviceStatus.EMERGENCY_ALERT.isAlert());
    }

    @Test
    public void noMotionAlertIsNotOffline() {
        assertFalse(DeviceStatus.NO_MOTION_ALERT == DeviceStatus.OFFLINE);
        assertFalse(DeviceStatus.EMERGENCY_ALERT == DeviceStatus.OFFLINE);
        assertEquals(DeviceStatus.NO_MOTION_ALERT, DeviceStatus.forAlertType(AlertType.NO_MOTION));
        assertEquals(DeviceStatus.EMERGENCY_ALERT, DeviceStatus.forAlertType(AlertType.EMERGENCY_BUTTON));
        assertNull(DeviceStatus.forAlertType(null));
    }

    @Test
    public void alertTypeIsRestoredFromItsName() {
        assertEquals(AlertType.NO_MOTION, AlertType.fromName("NO_MOTION"));
        assertEquals(AlertType.EMERGENCY_BUTTON, AlertType.fromName("EMERGENCY_BUTTON"));
        assertNull(AlertType.fromName("SOMETHING_ELSE"));
        assertNull(AlertType.fromName(null));
    }

    @Test
    public void alertRecordKeepsItsDemoFlag() {
        AlertRecord record = AlertRecord.create(AlertType.NO_MOTION, true);
        assertTrue(record.isDemo());
        assertEquals(AlertType.NO_MOTION, record.getType());
        assertTrue(record.getTimestampMillis() > 0);
    }
}

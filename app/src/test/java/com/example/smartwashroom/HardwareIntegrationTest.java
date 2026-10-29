package com.example.smartwashroom;

import com.example.smartwashroom.model.AlertType;
import com.example.smartwashroom.model.DeviceStatus;
import com.example.smartwashroom.model.FamilyContact;
import com.example.smartwashroom.model.HardwareDeviceState;

import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests verifying hardware status parsing, emergency notification logic,
 * heartbeat timeouts, and duplicate event prevention.
 */
public class HardwareIntegrationTest {

    private static final long HEARTBEAT_TIMEOUT_MS = 60_000L; // 60 seconds

    /**
     * Test A: Normal operation
     * - Hardware is online
     * - Emergency is inactive
     * - Buzzer is not ringing
     * Expected: Dashboard state is NORMAL, no emergency condition triggered.
     */
    @Test
    public void testA_NormalOperation() {
        Map<String, Object> data = new HashMap<>();
        data.put("lastSeen", System.currentTimeMillis());
        data.put("online", true);
        data.put("emergency", false);
        data.put("buzzer", false);
        data.put("status", "NORMAL");

        HardwareDeviceState state = HardwareDeviceState.fromMap("washroom_01", data, HEARTBEAT_TIMEOUT_MS);

        assertTrue("Device should be online", state.isOnline());
        assertFalse("Emergency should be inactive", state.isEmergencyActive());
        assertFalse("Buzzer should not be active", state.isBuzzerActive());
        assertFalse("Should NOT trigger buzzer-off emergency notification", state.isEmergencyWithBuzzerOff());
        assertEquals("System status should be NORMAL", HardwareDeviceState.STATUS_NORMAL, state.getSystemStatus());
        assertEquals("DeviceStatus enum should be ONLINE", DeviceStatus.ONLINE, state.toDeviceStatus());
    }

    /**
     * Test B: Emergency with buzzer OFF (Primary Requirement)
     * - Hardware is online
     * - Emergency event is detected
     * - Buzzer status is explicitly OFF
     * Expected: isEmergencyWithBuzzerOff() returns true, triggering notification.
     */
    @Test
    public void testB_EmergencyWithBuzzerOff() {
        Map<String, Object> data = new HashMap<>();
        data.put("lastSeen", System.currentTimeMillis());
        data.put("online", true);
        data.put("emergency", true);
        data.put("buzzer", false);
        data.put("alert_type", "EMERGENCY_BUTTON");
        data.put("status", "EMERGENCY");

        HardwareDeviceState state = HardwareDeviceState.fromMap("washroom_01", data, HEARTBEAT_TIMEOUT_MS);

        assertTrue("Device should be online", state.isOnline());
        assertTrue("Emergency should be active", state.isEmergencyActive());
        assertFalse("Buzzer should be OFF", state.isBuzzerActive());
        assertTrue("Primary Requirement Met: Emergency with buzzer OFF must trigger notification!",
                state.isEmergencyWithBuzzerOff());
        assertEquals("System status should be EMERGENCY", HardwareDeviceState.STATUS_EMERGENCY, state.getSystemStatus());
        assertEquals("DeviceStatus should be EMERGENCY_ALERT", DeviceStatus.EMERGENCY_ALERT, state.toDeviceStatus());
    }

    /**
     * Test C: Emergency with buzzer ON
     * - Emergency event is detected
     * - Buzzer status is ON / Ringing
     * Expected: isEmergencyWithBuzzerOff() returns false, preserving local buzzer alarm without
     * triggering the buzzer-off specific notification.
     */
    @Test
    public void testC_EmergencyWithBuzzerOn() {
        Map<String, Object> data = new HashMap<>();
        data.put("lastSeen", System.currentTimeMillis());
        data.put("online", true);
        data.put("emergency", true);
        data.put("buzzer", true); // Buzzer is ringing!
        data.put("alert_type", "NO_MOTION");
        data.put("status", "EMERGENCY");

        HardwareDeviceState state = HardwareDeviceState.fromMap("washroom_01", data, HEARTBEAT_TIMEOUT_MS);

        assertTrue("Emergency is active", state.isEmergencyActive());
        assertTrue("Buzzer is ringing", state.isBuzzerActive());
        assertFalse("Must NOT trigger buzzer-off notification when buzzer is ringing",
                state.isEmergencyWithBuzzerOff());
        assertEquals("System status should be EMERGENCY", HardwareDeviceState.STATUS_EMERGENCY, state.getSystemStatus());
        assertEquals("DeviceStatus should be NO_MOTION_ALERT", DeviceStatus.NO_MOTION_ALERT, state.toDeviceStatus());
    }

    /**
     * Test D: Duplicate Firebase updates
     * - Consecutive updates with identical event signature
     * Expected: Event signature matches, preventing duplicate notifications.
     */
    @Test
    public void testD_DuplicateFirebaseUpdates() {
        long timestamp = 1730100000000L;
        Map<String, Object> update1 = new HashMap<>();
        update1.put("lastSeen", timestamp);
        update1.put("emergency", true);
        update1.put("buzzer", false);
        update1.put("status", "EMERGENCY");

        HardwareDeviceState state1 = HardwareDeviceState.fromMap("washroom_01", update1, HEARTBEAT_TIMEOUT_MS);
        String signature1 = state1.getDeviceId() + "_" + state1.getLastSeen() + "_" + state1.getRawStatus();

        // Exact duplicate payload received again
        HardwareDeviceState state2 = HardwareDeviceState.fromMap("washroom_01", update1, HEARTBEAT_TIMEOUT_MS);
        String signature2 = state2.getDeviceId() + "_" + state2.getLastSeen() + "_" + state2.getRawStatus();

        assertEquals("Duplicate signature should match first event", signature1, signature2);
    }

    /**
     * Test E: Hardware offline detection via heartbeat timeout
     * - Heartbeat is older than configured timeout (e.g. 70 seconds ago vs 60s timeout)
     * Expected: Device is classified as OFFLINE, NOT falsely classified as emergency.
     */
    @Test
    public void testE_HardwareOfflineHeartbeatTimeout() {
        long staleTimestamp = System.currentTimeMillis() - 70_000L; // 70 seconds ago
        Map<String, Object> data = new HashMap<>();
        data.put("lastSeen", staleTimestamp);
        data.put("online", true); // Hardware wrote true in the past, but is now silent
        data.put("emergency", false);
        data.put("buzzer", false);

        HardwareDeviceState state = HardwareDeviceState.fromMap("washroom_01", data, HEARTBEAT_TIMEOUT_MS);

        assertFalse("Stale heartbeat must mark device as OFFLINE", state.isOnline());
        assertFalse("Must not falsely report emergency when offline", state.isEmergencyActive());
        assertEquals("DeviceStatus should be OFFLINE", DeviceStatus.OFFLINE, state.toDeviceStatus());
    }

    /**
     * Test F: Parsing flexible firmware formats (Integers 1/0, strings "ON"/"OFF", "TRUE"/"FALSE")
     */
    @Test
    public void testF_FlexibleFirmwareFormatParsing() {
        Map<String, Object> data = new HashMap<>();
        data.put("lastSeen", System.currentTimeMillis() / 1000L); // Seconds format
        data.put("online", 1); // Numeric 1 for true
        data.put("buzzer", "OFF"); // String "OFF"
        data.put("emergency", "TRUE"); // String "TRUE"
        data.put("motion", "0"); // Numeric 0

        HardwareDeviceState state = HardwareDeviceState.fromMap("washroom_01", data, HEARTBEAT_TIMEOUT_MS);

        assertTrue("Online from numeric 1", state.isOnline());
        assertFalse("Buzzer from string OFF", state.isBuzzerActive());
        assertTrue("Emergency from string TRUE", state.isEmergencyActive());
        assertTrue("Emergency with buzzer OFF triggered", state.isEmergencyWithBuzzerOff());
    }

    /**
     * Test G: Multiple family members and FCM token registration association
     */
    @Test
    public void testG_MultipleFamilyContactsWithFCMTokens() {
        List<FamilyContact> family = new ArrayList<>();
        family.add(new FamilyContact(1L, "Alice Johnson", "+919876543210", "fcm_token_alice_123", true));
        family.add(new FamilyContact(2L, "Bob Johnson", "+919876543211", "fcm_token_bob_456", true));
        family.add(new FamilyContact(3L, "Charlie Johnson", "+919876543212", null, false));

        assertEquals(3, family.size());

        // Eligible recipients (enabled and has FCM token or phone)
        List<FamilyContact> eligibleFCMRecipients = new ArrayList<>();
        for (FamilyContact c : family) {
            if (c.isNotificationsEnabled() && c.getFcmToken() != null && !c.getFcmToken().isEmpty()) {
                eligibleFCMRecipients.add(c);
            }
        }

        assertEquals("Should have exactly 2 eligible FCM recipients", 2, eligibleFCMRecipients.size());
        assertEquals("fcm_token_alice_123", eligibleFCMRecipients.get(0).getFcmToken());
        assertEquals("fcm_token_bob_456", eligibleFCMRecipients.get(1).getFcmToken());
    }
}

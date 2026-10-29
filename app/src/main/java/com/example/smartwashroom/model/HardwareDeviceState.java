package com.example.smartwashroom.model;

import com.google.firebase.database.DataSnapshot;

import java.util.Map;

/**
 * Represents the live hardware status received from Firebase.
 *
 * <p>Handles data fields published by the microcontroller (ESP8266 / ESP32)
 * and normalizes booleans, integers, and strings so different firmware implementations
 * are parsed safely and consistently.</p>
 */
public class HardwareDeviceState {

    public static final String STATUS_NORMAL = "NORMAL";
    public static final String STATUS_WARNING = "WARNING";
    public static final String STATUS_EMERGENCY = "EMERGENCY";

    private final String deviceId;
    private final long lastSeen;
    private final boolean online;
    private final boolean buzzerActive;
    private final boolean emergencyActive;
    private final boolean motionDetected;
    private final String occupancy;
    private final AlertType alertType;
    private final int batteryLevel;
    private final String rawStatus;

    public HardwareDeviceState(
            String deviceId,
            long lastSeen,
            boolean online,
            boolean buzzerActive,
            boolean emergencyActive,
            boolean motionDetected,
            String occupancy,
            AlertType alertType,
            int batteryLevel,
            String rawStatus) {
        this.deviceId = (deviceId == null || deviceId.isEmpty()) ? "washroom_01" : deviceId;
        this.lastSeen = lastSeen;
        this.online = online;
        this.buzzerActive = buzzerActive;
        this.emergencyActive = emergencyActive;
        this.motionDetected = motionDetected;
        this.occupancy = (occupancy == null || occupancy.isEmpty()) ? "UNKNOWN" : occupancy;
        this.alertType = alertType;
        this.batteryLevel = batteryLevel;
        this.rawStatus = (rawStatus == null || rawStatus.isEmpty()) ? "NORMAL" : rawStatus;
    }

    /** Default empty/offline state. */
    public static HardwareDeviceState createDefault(String deviceId) {
        return new HardwareDeviceState(
                deviceId,
                0L,
                false,
                false,
                false,
                false,
                "UNKNOWN",
                null,
                -1,
                "OFFLINE"
        );
    }

    public String getDeviceId() {
        return deviceId;
    }

    public long getLastSeen() {
        return lastSeen;
    }

    public boolean isOnline() {
        return online;
    }

    public boolean isBuzzerActive() {
        return buzzerActive;
    }

    public boolean isEmergencyActive() {
        return emergencyActive;
    }

    public boolean isMotionDetected() {
        return motionDetected;
    }

    public String getOccupancy() {
        return occupancy;
    }

    public AlertType getAlertType() {
        return alertType;
    }

    public int getBatteryLevel() {
        return batteryLevel;
    }

    public String getRawStatus() {
        return rawStatus;
    }

    /**
     * Core safety requirement:
     * When an emergency condition is detected by the hardware and the buzzer is NOT ringing.
     */
    public boolean isEmergencyWithBuzzerOff() {
        return emergencyActive && !buzzerActive;
    }

    /**
     * Determines current system status: Normal, Warning, or Emergency.
     */
    public String getSystemStatus() {
        if (emergencyActive || alertType == AlertType.EMERGENCY_BUTTON) {
            return STATUS_EMERGENCY;
        }
        if (alertType == AlertType.NO_MOTION || !online) {
            return STATUS_WARNING;
        }
        return STATUS_NORMAL;
    }

    /**
     * Maps the hardware state to the existing DeviceStatus enum for backward compatibility.
     */
    public DeviceStatus toDeviceStatus() {
        if (alertType == AlertType.EMERGENCY_BUTTON) {
            return DeviceStatus.EMERGENCY_ALERT;
        }
        if (alertType == AlertType.NO_MOTION) {
            return DeviceStatus.NO_MOTION_ALERT;
        }
        if (emergencyActive) {
            return DeviceStatus.EMERGENCY_ALERT;
        }
        return online ? DeviceStatus.ONLINE : DeviceStatus.OFFLINE;
    }

    /**
     * Parses hardware data from a Firebase Realtime Database DataSnapshot.
     */
    public static HardwareDeviceState fromSnapshot(DataSnapshot snapshot, long heartbeatTimeoutMs) {
        if (snapshot == null || !snapshot.exists()) {
            return createDefault(snapshot != null ? snapshot.getKey() : "washroom_01");
        }

        String deviceId = snapshot.getKey();

        // 1. Timestamp / Heartbeat
        long lastSeen = extractLong(snapshot, "lastSeen", "heartbeat", "timestamp", "last_updated", "updatedAt");
        long now = System.currentTimeMillis();

        // Check if timestamp is in seconds (10 digits) or milliseconds (13 digits)
        if (lastSeen >= 1000000000L && lastSeen < 10000000000L) {
            lastSeen *= 1000L;
        }

        // Offline detection: hardware heartbeat staleness
        // If lastSeen is real UTC epoch ms (>= 1,000,000,000,000 ms), compare against phone time.
        // If lastSeen is ESP uptime millis (< 1,000,000,000), don't penalize it as a 1970 timestamp.
        boolean isEpochTimestamp = (lastSeen >= 1000000000000L);
        boolean heartBeatAlive = isEpochTimestamp ? ((now - lastSeen) <= heartbeatTimeoutMs) : true;

        // Explicit status string/bool check
        Boolean onlineVal = extractBoolean(snapshot, "online", "isOnline", "connected");
        boolean online = (onlineVal != null) ? (onlineVal && heartBeatAlive) : heartBeatAlive;

        // If uptime or no valid epoch timestamp was written, fall back to now if device is online
        if (!isEpochTimestamp && onlineVal != null && onlineVal) {
            online = true;
            lastSeen = now;
        }

        // 2. Buzzer status (Ringing vs Not Ringing)
        Boolean buzzerVal = extractBoolean(snapshot, "buzzer", "buzzer_status", "is_buzzer", "buzzerStatus", "alarm");
        boolean buzzerActive = buzzerVal != null && buzzerVal;

        // 3. Emergency / Fall status
        Boolean emergencyVal = extractBoolean(snapshot, "emergency", "is_emergency", "emergency_status", "fall", "isFall");
        String statusStr = extractString(snapshot, "status", "system_status", "state");
        boolean emergencyActive = (emergencyVal != null && emergencyVal)
                || "EMERGENCY".equalsIgnoreCase(statusStr)
                || "FALL".equalsIgnoreCase(statusStr)
                || "ALERT".equalsIgnoreCase(statusStr);

        // 4. Alert Type
        String typeStr = extractString(snapshot, "alert_type", "type", "alertType");
        AlertType alertType = AlertType.fromName(typeStr);
        if (alertType == null && emergencyActive) {
            if ("FALL".equalsIgnoreCase(typeStr) || "NO_MOTION".equalsIgnoreCase(typeStr)) {
                alertType = AlertType.NO_MOTION;
            } else {
                alertType = AlertType.EMERGENCY_BUTTON;
            }
        }

        // 5. Motion / Occupancy
        Boolean motionVal = extractBoolean(snapshot, "motion", "motion_detected", "motionDetected", "pir");
        boolean motionDetected = motionVal != null && motionVal;

        String occupancy = extractString(snapshot, "occupancy", "occupancy_status", "presence");
        if (occupancy == null || occupancy.isEmpty()) {
            occupancy = motionDetected ? "OCCUPIED" : "VACANT";
        }

        // 6. Battery level
        long batteryLong = extractLong(snapshot, "batteryLevel", "battery", "bat");
        int batteryLevel = batteryLong >= 0 ? (int) batteryLong : -1;

        return new HardwareDeviceState(
                deviceId,
                lastSeen,
                online,
                buzzerActive,
                emergencyActive,
                motionDetected,
                occupancy,
                alertType,
                batteryLevel,
                statusStr != null ? statusStr : (emergencyActive ? STATUS_EMERGENCY : (online ? STATUS_NORMAL : "OFFLINE"))
        );
    }

    /**
     * Parses hardware data from a generic Map (useful for unit tests and Cloud Event payloads).
     */
    public static HardwareDeviceState fromMap(String deviceId, Map<String, Object> map, long heartbeatTimeoutMs) {
        if (map == null || map.isEmpty()) {
            return createDefault(deviceId);
        }

        long lastSeen = extractLongFromMap(map, "lastSeen", "heartbeat", "timestamp", "last_updated", "updatedAt");
        long now = System.currentTimeMillis();

        if (lastSeen >= 1000000000L && lastSeen < 10000000000L) {
            lastSeen *= 1000L;
        }

        boolean isEpochTimestamp = (lastSeen >= 1000000000000L);
        boolean heartBeatAlive = isEpochTimestamp ? ((now - lastSeen) <= heartbeatTimeoutMs) : true;

        Boolean onlineVal = extractBooleanFromMap(map, "online", "isOnline", "connected");
        boolean online = (onlineVal != null) ? (onlineVal && heartBeatAlive) : heartBeatAlive;

        if (!isEpochTimestamp && onlineVal != null && onlineVal) {
            online = true;
            lastSeen = now;
        }

        Boolean buzzerVal = extractBooleanFromMap(map, "buzzer", "buzzer_status", "is_buzzer", "buzzerStatus", "alarm");
        boolean buzzerActive = buzzerVal != null && buzzerVal;

        Boolean emergencyVal = extractBooleanFromMap(map, "emergency", "is_emergency", "emergency_status", "fall", "isFall");
        String statusStr = extractStringFromMap(map, "status", "system_status", "state");
        boolean emergencyActive = (emergencyVal != null && emergencyVal)
                || "EMERGENCY".equalsIgnoreCase(statusStr)
                || "FALL".equalsIgnoreCase(statusStr)
                || "ALERT".equalsIgnoreCase(statusStr);

        String typeStr = extractStringFromMap(map, "alert_type", "type", "alertType");
        AlertType alertType = AlertType.fromName(typeStr);
        if (alertType == null && emergencyActive) {
            if ("FALL".equalsIgnoreCase(typeStr) || "NO_MOTION".equalsIgnoreCase(typeStr)) {
                alertType = AlertType.NO_MOTION;
            } else {
                alertType = AlertType.EMERGENCY_BUTTON;
            }
        }

        Boolean motionVal = extractBooleanFromMap(map, "motion", "motion_detected", "motionDetected", "pir");
        boolean motionDetected = motionVal != null && motionVal;

        String occupancy = extractStringFromMap(map, "occupancy", "occupancy_status", "presence");
        if (occupancy == null || occupancy.isEmpty()) {
            occupancy = motionDetected ? "OCCUPIED" : "VACANT";
        }

        long batteryLong = extractLongFromMap(map, "batteryLevel", "battery", "bat");
        int batteryLevel = batteryLong >= 0 ? (int) batteryLong : -1;

        return new HardwareDeviceState(
                deviceId,
                lastSeen,
                online,
                buzzerActive,
                emergencyActive,
                motionDetected,
                occupancy,
                alertType,
                batteryLevel,
                statusStr != null ? statusStr : (emergencyActive ? STATUS_EMERGENCY : (online ? STATUS_NORMAL : "OFFLINE"))
        );
    }

    // Helper extractors supporting flexible types (boolean, number, string)
    private static Boolean extractBoolean(DataSnapshot snapshot, String... keys) {
        for (String key : keys) {
            if (snapshot.hasChild(key)) {
                Object val = snapshot.child(key).getValue();
                Boolean parsed = parseBooleanValue(val);
                if (parsed != null) return parsed;
            }
        }
        return null;
    }

    private static long extractLong(DataSnapshot snapshot, String... keys) {
        for (String key : keys) {
            if (snapshot.hasChild(key)) {
                Object val = snapshot.child(key).getValue();
                Long parsed = parseLongValue(val);
                if (parsed != null) return parsed;
            }
        }
        return 0L;
    }

    private static String extractString(DataSnapshot snapshot, String... keys) {
        for (String key : keys) {
            if (snapshot.hasChild(key)) {
                Object val = snapshot.child(key).getValue();
                if (val != null) return val.toString().trim();
            }
        }
        return null;
    }

    private static Boolean extractBooleanFromMap(Map<String, Object> map, String... keys) {
        for (String key : keys) {
            if (map.containsKey(key)) {
                Boolean parsed = parseBooleanValue(map.get(key));
                if (parsed != null) return parsed;
            }
        }
        return null;
    }

    private static long extractLongFromMap(Map<String, Object> map, String... keys) {
        for (String key : keys) {
            if (map.containsKey(key)) {
                Long parsed = parseLongValue(map.get(key));
                if (parsed != null) return parsed;
            }
        }
        return 0L;
    }

    private static String extractStringFromMap(Map<String, Object> map, String... keys) {
        for (String key : keys) {
            if (map.containsKey(key)) {
                Object val = map.get(key);
                if (val != null) return val.toString().trim();
            }
        }
        return null;
    }

    public static Boolean parseBooleanValue(Object val) {
        if (val == null) return null;
        if (val instanceof Boolean) return (Boolean) val;
        if (val instanceof Number) return ((Number) val).intValue() != 0;
        String s = val.toString().trim().toUpperCase();
        if (s.equals("TRUE") || s.equals("ON") || s.equals("RINGING") || s.equals("1") || s.equals("ACTIVE") || s.equals("YES")) {
            return true;
        }
        if (s.equals("FALSE") || s.equals("OFF") || s.equals("NOT_RINGING") || s.equals("0") || s.equals("INACTIVE") || s.equals("NO")) {
            return false;
        }
        return null;
    }

    public static Long parseLongValue(Object val) {
        if (val == null) return null;
        if (val instanceof Number) return ((Number) val).longValue();
        try {
            return Long.parseLong(val.toString().trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}

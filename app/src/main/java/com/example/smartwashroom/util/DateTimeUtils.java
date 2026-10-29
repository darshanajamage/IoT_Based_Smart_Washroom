package com.example.smartwashroom.util;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Formats the alert timestamps in a way that is easy to read on a phone.
 */
public final class DateTimeUtils {

    private DateTimeUtils() {
        // No instances - this class only has static helpers.
    }

    /** @return for example "10:32 PM". */
    public static String formatTime(long timestampMillis) {
        return new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(new Date(timestampMillis));
    }

    /** @return for example "21 Oct 2026". */
    public static String formatDate(long timestampMillis) {
        return new SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(new Date(timestampMillis));
    }

    /** @return for example "21 Oct 2026, 10:32 PM". */
    public static String formatDateTime(long timestampMillis) {
        return formatDate(timestampMillis) + ", " + formatTime(timestampMillis);
    }
}

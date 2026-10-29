package com.example.smartwashroom.util;

import java.util.regex.Pattern;

/**
 * Small helper that checks whether a phone number looks usable.
 *
 * <p>The check is deliberately simple. It is not a full international phone number
 * parser (that needs the libphonenumber library), it only makes sure the number
 * contains enough digits to be worth calling and no letters.</p>
 */
public final class PhoneNumberValidator {

    /**
     * A phone number may contain an optional "+" (country code), digits and the
     * usual formatting characters: space, dash and brackets.
     */
    private static final Pattern ALLOWED_CHARACTERS = Pattern.compile("^[+]?[0-9()\\- ]+$");

    /** The shortest number we accept (for example "56789"). */
    private static final int MIN_DIGITS = 5;

    /** The longest number accepted by the international dialling plan. */
    private static final int MAX_DIGITS = 15;

    private PhoneNumberValidator() {
        // No instances - this class only has static helpers.
    }

    /** @return true when the text is null, empty or only white space. */
    public static boolean isEmpty(String text) {
        return text == null || text.trim().isEmpty();
    }

    /**
     * @return true when the number looks like a phone number that can be dialled.
     */
    public static boolean isValid(String phoneNumber) {
        if (isEmpty(phoneNumber)) {
            return false;
        }
        String trimmed = phoneNumber.trim();
        if (!ALLOWED_CHARACTERS.matcher(trimmed).matches()) {
            return false;
        }
        if (!bracketsAreBalanced(trimmed)) {
            return false;
        }
        int digits = countDigits(trimmed);
        return digits >= MIN_DIGITS && digits <= MAX_DIGITS;
    }

    /**
     * @return true when every "(" has a matching ")" and they are in the right
     *         order, so "(987) 654-3210" is fine but "(987 654)3210" is not.
     */
    private static boolean bracketsAreBalanced(String phoneNumber) {
        int open = 0;
        for (int i = 0; i < phoneNumber.length(); i++) {
            char c = phoneNumber.charAt(i);
            if (c == '(') {
                open++;
            } else if (c == ')') {
                open--;
                if (open < 0) {
                    return false;
                }
            }
        }
        return open == 0;
    }

    /** @return how many digits the text contains. */
    public static int countDigits(String text) {
        if (text == null) {
            return 0;
        }
        int digits = 0;
        for (int i = 0; i < text.length(); i++) {
            if (Character.isDigit(text.charAt(i))) {
                digits++;
            }
        }
        return digits;
    }

    /**
     * Prepares a number for the dialler.
     *
     * <p>Everything that is not a digit is removed, but a leading "+" (used for
     * country codes) is kept.</p>
     */
    public static String normalizeForDial(String phoneNumber) {
        if (phoneNumber == null) {
            return "";
        }
        String trimmed = phoneNumber.trim();
        StringBuilder builder = new StringBuilder();
        if (trimmed.startsWith("+")) {
            builder.append('+');
        }
        for (int i = 0; i < trimmed.length(); i++) {
            char c = trimmed.charAt(i);
            if (Character.isDigit(c)) {
                builder.append(c);
            }
        }
        return builder.toString();
    }
}

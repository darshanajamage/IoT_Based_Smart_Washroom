package com.example.smartwashroom.util;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;

import com.example.smartwashroom.R;

/**
 * Opens the phone dialler for a contact.
 *
 * <p>The app uses {@link Intent#ACTION_DIAL} on purpose: the number is only
 * <b>prepared</b> in the dialler and the user still has to press the green call
 * button. That is safer than placing the call automatically, and it does not
 * need the {@code CALL_PHONE} permission.</p>
 */
public final class DialerHelper {

    private DialerHelper() {
        // No instances - this class only has static helpers.
    }

    /**
     * Opens the dialler with the given number.
     *
     * @return true when the dialler could be opened.
     */
    public static boolean dial(Context context, String phoneNumber) {
        String number = PhoneNumberValidator.normalizeForDial(phoneNumber);
        if (number.isEmpty()) {
            Toast.makeText(context, R.string.contact_phone_error, Toast.LENGTH_SHORT).show();
            return false;
        }

        Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + number));
        if (!(context instanceof Activity)) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }

        try {
            context.startActivity(intent);
            return true;
        } catch (ActivityNotFoundException e) {
            // Some devices (tablets without a SIM) have no dialler at all.
            Toast.makeText(context, R.string.contact_no_dialer, Toast.LENGTH_LONG).show();
            return false;
        }
    }
}

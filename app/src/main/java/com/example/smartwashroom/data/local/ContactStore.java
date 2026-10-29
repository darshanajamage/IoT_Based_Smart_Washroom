package com.example.smartwashroom.data.local;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.smartwashroom.model.FamilyContact;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Stores the family contacts on this phone.
 *
 * <p>The contacts are kept in {@link SharedPreferences} as a small JSON array, so
 * they survive an app restart, an app update and even a device reboot. Nothing is
 * sent to a server - the contact list is private to the phone.</p>
 *
 * <p>This class is a "store": it only knows how to read and write the raw data.
 * The screens talk to {@link com.example.smartwashroom.data.ContactRepository},
 * which sits on top of it.</p>
 */
public class ContactStore {

    private static final String PREFS_NAME = "family_contacts";
    private static final String KEY_CONTACTS = "contacts_json";

    private final SharedPreferences preferences;

    public ContactStore(Context context) {
        this.preferences = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    /** @return every saved contact, in the order they were added. */
    public List<FamilyContact> getContacts() {
        List<FamilyContact> contacts = new ArrayList<>();
        String raw = preferences.getString(KEY_CONTACTS, null);
        if (raw == null || raw.isEmpty()) {
            return contacts;
        }
        try {
            JSONArray array = new JSONArray(raw);
            for (int i = 0; i < array.length(); i++) {
                JSONObject json = array.optJSONObject(i);
                FamilyContact contact = FamilyContact.fromJson(json);
                if (contact != null) {
                    contacts.add(contact);
                }
            }
        } catch (Exception e) {
            // A broken entry must never crash the app - just start with no contacts.
            return new ArrayList<>();
        }
        return contacts;
    }

    /** Replaces the whole list with the given contacts. */
    public void saveContacts(List<FamilyContact> contacts) {
        JSONArray array = new JSONArray();
        try {
            for (FamilyContact contact : contacts) {
                array.put(contact.toJson());
            }
        } catch (Exception e) {
            // Should never happen, but we keep the old list rather than losing it.
            return;
        }
        preferences.edit().putString(KEY_CONTACTS, array.toString()).apply();
    }

    /** @return a new, unique id for a contact that is about to be added. */
    public long generateId(List<FamilyContact> existing) {
        long id = System.currentTimeMillis();
        boolean stillUsed;
        do {
            stillUsed = false;
            for (FamilyContact contact : existing) {
                if (contact.getId() == id) {
                    stillUsed = true;
                    id++;
                    break;
                }
            }
        } while (stillUsed);
        return id;
    }
}

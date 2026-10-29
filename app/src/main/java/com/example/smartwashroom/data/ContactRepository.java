package com.example.smartwashroom.data;

import android.content.Context;
import android.util.Log;

import com.example.smartwashroom.data.local.ContactStore;
import com.example.smartwashroom.model.FamilyContact;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Stores and manages family contacts, supporting local storage and Firebase synchronization
 * so the backend can dispatch emergency push notifications to all registered family members.
 */
public class ContactRepository {

    private static final String TAG = "ContactRepository";
    private static volatile ContactRepository instance;

    private final ContactStore contactStore;

    private ContactRepository(Context context) {
        this.contactStore = new ContactStore(context);
    }

    public static ContactRepository getInstance(Context context) {
        if (instance == null) {
            synchronized (ContactRepository.class) {
                if (instance == null) {
                    instance = new ContactRepository(context);
                }
            }
        }
        return instance;
    }

    /** @return every saved contact. */
    public List<FamilyContact> getContacts() {
        return contactStore.getContacts();
    }

    /** @return the contact with this id, or {@code null}. */
    public FamilyContact getContact(long id) {
        for (FamilyContact contact : contactStore.getContacts()) {
            if (contact.getId() == id) {
                return contact;
            }
        }
        return null;
    }

    /**
     * Adds a new contact and syncs to Firebase.
     */
    public FamilyContact addContact(String name, String phoneNumber) {
        List<FamilyContact> contacts = contactStore.getContacts();
        FamilyContact contact = new FamilyContact(contactStore.generateId(contacts), name, phoneNumber);
        contacts.add(contact);
        contactStore.saveContacts(contacts);
        syncContactToFirebase(contact);
        return contact;
    }

    /**
     * Updates an existing contact and syncs to Firebase.
     */
    public boolean updateContact(FamilyContact contact) {
        if (contact == null) {
            return false;
        }
        List<FamilyContact> contacts = contactStore.getContacts();
        for (int i = 0; i < contacts.size(); i++) {
            if (contacts.get(i).getId() == contact.getId()) {
                contacts.set(i, contact);
                contactStore.saveContacts(contacts);
                syncContactToFirebase(contact);
                return true;
            }
        }
        return false;
    }

    /**
     * Deletes a contact and removes from Firebase.
     */
    public boolean deleteContact(long id) {
        List<FamilyContact> contacts = contactStore.getContacts();
        boolean removed = false;
        for (int i = contacts.size() - 1; i >= 0; i--) {
            if (contacts.get(i).getId() == id) {
                contacts.remove(i);
                removed = true;
            }
        }
        if (removed) {
            contactStore.saveContacts(contacts);
            removeContactFromFirebase(id);
        }
        return removed;
    }

    /**
     * Syncs an individual contact to Firebase Realtime Database.
     */
    private void syncContactToFirebase(FamilyContact contact) {
        try {
            DatabaseReference contactRef = FirebaseDatabase.getInstance()
                    .getReference(FirebaseConfig.PATH_FAMILY_CONTACTS)
                    .child(String.valueOf(contact.getId()));

            Map<String, Object> map = new HashMap<>();
            map.put("id", contact.getId());
            map.put("name", contact.getName());
            map.put("phone", contact.getPhoneNumber());
            map.put("notificationsEnabled", contact.isNotificationsEnabled());
            if (contact.getFcmToken() != null) {
                map.put("fcmToken", contact.getFcmToken());
            }

            contactRef.setValue(map);
        } catch (Exception e) {
            Log.w(TAG, "Failed to sync contact to Firebase: " + e.getMessage());
        }
    }

    private void removeContactFromFirebase(long id) {
        try {
            FirebaseDatabase.getInstance()
                    .getReference(FirebaseConfig.PATH_FAMILY_CONTACTS)
                    .child(String.valueOf(id))
                    .removeValue();
        } catch (Exception e) {
            Log.w(TAG, "Failed to remove contact from Firebase: " + e.getMessage());
        }
    }
}

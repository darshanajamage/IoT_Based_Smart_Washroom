package com.example.smartwashroom.ui;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.smartwashroom.R;
import com.example.smartwashroom.data.ContactRepository;
import com.example.smartwashroom.model.FamilyContact;
import com.example.smartwashroom.util.DialerHelper;

import java.util.List;

/**
 * FAMILY CONTACTS screen.
 *
 * <p>Shows the contacts that were saved on this phone. Every contact has a
 * Call button (opens the dialler), an Edit button and a Delete button. New
 * contacts are added with the floating action button.</p>
 */
public class ContactsActivity extends BaseActivity {

    private ContactRepository repository;

    private LinearLayout contactContainer;
    private View emptyPanel;

    @Override
    protected int getLayoutResourceId() {
        return R.layout.activity_contacts;
    }

    @Override
    protected String getToolbarTitle() {
        return getString(R.string.contacts_title);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        repository = ContactRepository.getInstance(this);

        contactContainer = findViewById(R.id.contact_container);
        emptyPanel = findViewById(R.id.empty_contacts_panel);

        findViewById(R.id.fab_add_contact).setOnClickListener(view -> openContactEditor(null));
        findViewById(R.id.btn_add_first_contact).setOnClickListener(view -> openContactEditor(null));
    }

    @Override
    protected void onResume() {
        super.onResume();
        // The list is reloaded every time, so a contact that was added or edited
        // in the other screen is shown immediately.
        showContacts();
    }

    /** Builds the list of contacts. */
    private void showContacts() {
        contactContainer.removeAllViews();
        List<FamilyContact> contacts = repository.getContacts();

        boolean empty = contacts.isEmpty();
        emptyPanel.setVisibility(empty ? View.VISIBLE : View.GONE);
        contactContainer.setVisibility(empty ? View.GONE : View.VISIBLE);

        LayoutInflater inflater = LayoutInflater.from(this);
        for (final FamilyContact contact : contacts) {
            View row = inflater.inflate(R.layout.item_contact, contactContainer, false);

            TextView nameText = row.findViewById(R.id.tv_contact_name);
            TextView phoneText = row.findViewById(R.id.tv_contact_phone);
            nameText.setText(contact.getName());
            phoneText.setText(contact.getPhoneNumber());

            ImageButton callButton = row.findViewById(R.id.btn_call_contact);
            ImageButton editButton = row.findViewById(R.id.btn_edit_contact);
            ImageButton deleteButton = row.findViewById(R.id.btn_delete_contact);

            callButton.setOnClickListener(view -> confirmCall(contact));
            editButton.setOnClickListener(view -> openContactEditor(contact));
            deleteButton.setOnClickListener(view -> confirmDelete(contact));

            contactContainer.addView(row);
        }
    }

    /** Opens the add / edit screen for the given contact (null means "add"). */
    private void openContactEditor(FamilyContact contact) {
        Intent intent = new Intent(this, ContactEditActivity.class);
        if (contact != null) {
            intent.putExtra(ContactEditActivity.EXTRA_CONTACT_ID, contact.getId());
        }
        startActivity(intent);
    }

    /** Asks before opening the dialler. */
    private void confirmCall(FamilyContact contact) {
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.contact_call_title, contact.getName()))
                .setMessage(getString(R.string.contact_call_message, contact.getPhoneNumber()))
                .setPositiveButton(R.string.action_call, (dialog, which) ->
                        DialerHelper.dial(this, contact.getPhoneNumber()))
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }

    /** Asks before deleting a contact. */
    private void confirmDelete(FamilyContact contact) {
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.contact_delete_title, contact.getName()))
                .setMessage(R.string.contact_delete_message)
                .setPositiveButton(R.string.action_delete, (dialog, which) -> {
                    repository.deleteContact(contact.getId());
                    showToast(getString(R.string.contact_deleted));
                    showContacts();
                })
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }
}

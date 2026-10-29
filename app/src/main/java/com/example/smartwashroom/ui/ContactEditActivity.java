package com.example.smartwashroom.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.example.smartwashroom.R;
import com.example.smartwashroom.data.ContactRepository;
import com.example.smartwashroom.model.FamilyContact;
import com.example.smartwashroom.util.PhoneNumberValidator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

/**
 * ADD / EDIT a family member.
 *
 * <p>The same screen is used for a new contact and for editing an existing one.
 * The name and the phone number are checked before saving, so an empty form or a
 * wrong phone number can never be stored.</p>
 */
public class ContactEditActivity extends BaseActivity {

    /** Extra: the id of the contact that should be edited (-1 for a new contact). */
    public static final String EXTRA_CONTACT_ID = "extra_contact_id";

    private static final long NO_CONTACT = -1L;

    private ContactRepository repository;

    private TextInputLayout nameLayout;
    private TextInputLayout phoneLayout;
    private TextInputEditText nameInput;
    private TextInputEditText phoneInput;
    private Button saveButton;

    private long contactId = NO_CONTACT;
    private FamilyContact existingContact;

    @Override
    protected int getLayoutResourceId() {
        return R.layout.activity_contact_edit;
    }

    @Override
    protected String getToolbarTitle() {
        return getString(contactId == NO_CONTACT ? R.string.contact_add_title : R.string.contact_edit_title);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // The id is read before super.onCreate() because the app bar title
        // depends on whether a new contact is added or an existing one is edited.
        contactId = getIntent().getLongExtra(EXTRA_CONTACT_ID, NO_CONTACT);

        super.onCreate(savedInstanceState);
        repository = ContactRepository.getInstance(this);

        nameLayout = findViewById(R.id.til_name);
        phoneLayout = findViewById(R.id.til_phone);
        nameInput = findViewById(R.id.et_name);
        phoneInput = findViewById(R.id.et_phone);
        saveButton = findViewById(R.id.btn_save_contact);

        if (contactId != NO_CONTACT) {
            existingContact = repository.getContact(contactId);
        }

        if (existingContact != null) {
            nameInput.setText(existingContact.getName());
            phoneInput.setText(existingContact.getPhoneNumber());
            saveButton.setText(R.string.action_update_contact);
        }

        saveButton.setOnClickListener(view -> saveContact());
    }

    /** Checks the form and saves the contact. */
    private void saveContact() {
        String name = nameInput.getText() == null ? "" : nameInput.getText().toString().trim();
        String phone = phoneInput.getText() == null ? "" : phoneInput.getText().toString().trim();

        boolean valid = true;

        if (name.isEmpty()) {
            nameLayout.setError(getString(R.string.contact_name_error));
            valid = false;
        } else {
            nameLayout.setError(null);
        }

        if (PhoneNumberValidator.isEmpty(phone)) {
            phoneLayout.setError(getString(R.string.contact_phone_error));
            valid = false;
        } else if (!PhoneNumberValidator.isValid(phone)) {
            phoneLayout.setError(getString(R.string.contact_phone_error_digits));
            valid = false;
        } else {
            phoneLayout.setError(null);
        }

        if (!valid) {
            return;
        }

        if (existingContact != null) {
            existingContact.setName(name);
            existingContact.setPhoneNumber(phone);
            repository.updateContact(existingContact);
            showToast(getString(R.string.contact_updated));
        } else {
            repository.addContact(name, phone);
            showToast(getString(R.string.contact_saved));
        }

        finish();
    }
}

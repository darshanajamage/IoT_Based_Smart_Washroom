package com.example.smartwashroom.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Tests for the phone number check. The rule of the test is simple: a valid
 * number may be dialled, an invalid number must be refused.
 */
public class PhoneNumberValidatorTest {

    @Test
    public void emptyValuesAreNotValid() {
        assertTrue(PhoneNumberValidator.isEmpty(""));
        assertTrue(PhoneNumberValidator.isEmpty("   "));
        assertTrue(PhoneNumberValidator.isEmpty(null));

        assertFalse(PhoneNumberValidator.isValid(""));
        assertFalse(PhoneNumberValidator.isValid("   "));
        assertFalse(PhoneNumberValidator.isValid(null));
    }

    @Test
    public void normalNumbersAreValid() {
        assertTrue(PhoneNumberValidator.isValid("9876543210"));
        assertTrue(PhoneNumberValidator.isValid("98765 43210"));
        assertTrue(PhoneNumberValidator.isValid("98765-43210"));
        assertTrue(PhoneNumberValidator.isValid("(987) 654-3210"));
        assertTrue(PhoneNumberValidator.isValid("+91 98765 43210"));
    }

    @Test
    public void wrongNumbersAreNotValid() {
        // Too few digits
        assertFalse(PhoneNumberValidator.isValid("1234"));
        // Letters
        assertFalse(PhoneNumberValidator.isValid("98765abcde"));
        // A name instead of a number
        assertFalse(PhoneNumberValidator.isValid("Anita"));
        // Too many digits
        assertFalse(PhoneNumberValidator.isValid("1234567890123456789"));
        // Broken brackets
        assertFalse(PhoneNumberValidator.isValid("(987 6543210"));
        assertFalse(PhoneNumberValidator.isValid(")9876543210("));
    }

    @Test
    public void digitsAreCountedCorrectly() {
        assertEquals(10, PhoneNumberValidator.countDigits("98765 43210"));
        assertEquals(12, PhoneNumberValidator.countDigits("+91 98765 43210"));
        assertEquals(0, PhoneNumberValidator.countDigits(null));
    }

    @Test
    public void numberIsCleanedUpForTheDialler() {
        assertEquals("9876543210", PhoneNumberValidator.normalizeForDial("98765 43210"));
        assertEquals("9876543210", PhoneNumberValidator.normalizeForDial("(987) 654-3210"));
        assertEquals("+919876543210", PhoneNumberValidator.normalizeForDial("+91 98765 43210"));
        assertEquals("", PhoneNumberValidator.normalizeForDial(null));
    }
}

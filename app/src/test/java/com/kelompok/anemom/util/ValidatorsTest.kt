package com.kelompok.anemom.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ValidatorsTest {

    @Test
    fun email_validAndInvalid() {
        assertTrue(Validators.isValidEmail("rani.pratiwi@email.com"))
        assertFalse(Validators.isValidEmail("rani@email"))
        assertFalse(Validators.isValidEmail("rani.email.com"))
    }

    @Test
    fun phone_validAndInvalid() {
        assertTrue(Validators.isValidPhone("0812 3456 7890"))
        assertFalse(Validators.isValidPhone("123"))
        assertFalse(Validators.isValidPhone("0812abc34567"))
    }

    @Test
    fun password_minimumEightCharacters() {
        assertTrue(Validators.isValidPassword("12345678"))
        assertFalse(Validators.isValidPassword("1234567"))
    }
}

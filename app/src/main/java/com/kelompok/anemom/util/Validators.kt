package com.kelompok.anemom.util

object Validators {

    private val EMAIL_REGEX = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

    fun isValidEmail(value: String): Boolean = EMAIL_REGEX.matches(value.trim())

    /** Nomor telepon: hanya angka, spasi, '+' atau '-', dengan 9-15 digit. */
    fun isValidPhone(value: String): Boolean {
        val allowedOnly = value.all { it.isDigit() || it == ' ' || it == '+' || it == '-' }
        val digits = value.count { it.isDigit() }
        return allowedOnly && digits in 9..15
    }

    fun isValidPassword(value: String): Boolean = value.length >= 8
}

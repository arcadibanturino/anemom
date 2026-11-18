package com.kelompok.anemom.util

import java.time.LocalDate
import java.time.Period

/** Usia dari tanggal lahir, dan batas wajar usia ibu hamil untuk validasi form. */
object AgeCalculator {

    const val MIN_AGE = 10
    const val MAX_AGE = 60

    fun ageYears(birthDate: LocalDate, today: LocalDate = LocalDate.now()): Int =
        Period.between(birthDate, today).years

    /** Pesan galat untuk tanggal lahir, atau null jika valid. */
    fun validate(birthDate: LocalDate?, today: LocalDate = LocalDate.now()): String? = when {
        birthDate == null -> "Pilih tanggal lahir"
        birthDate.isAfter(today) -> "Tanggal lahir tidak boleh di masa depan"
        ageYears(birthDate, today) < MIN_AGE -> "Usia minimal $MIN_AGE tahun"
        ageYears(birthDate, today) > MAX_AGE -> "Usia maksimal $MAX_AGE tahun"
        else -> null
    }
}

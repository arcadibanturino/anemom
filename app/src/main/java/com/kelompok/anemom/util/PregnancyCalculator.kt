package com.kelompok.anemom.util

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** Perhitungan kehamilan sederhana berdasarkan HPHT (Hari Pertama Haid Terakhir). */
object PregnancyCalculator {

    const val MAX_WEEKS = 42

    fun weeksSince(hpht: LocalDate, today: LocalDate = LocalDate.now()): Int =
        (ChronoUnit.DAYS.between(hpht, today) / 7).toInt()

    /** Trimester 1: minggu 0-13, trimester 2: minggu 14-27, trimester 3: minggu 28 ke atas. */
    fun trimesterFor(weeks: Int): Int = when {
        weeks < 14 -> 1
        weeks < 28 -> 2
        else -> 3
    }

    /** Hari Perkiraan Lahir memakai rumus Naegele (HPHT + 280 hari). */
    fun dueDate(hpht: LocalDate): LocalDate = hpht.plusDays(280)

    fun trimesterLabel(trimester: Int): String = when (trimester) {
        1 -> "Trimester pertama"
        2 -> "Trimester kedua"
        else -> "Trimester ketiga"
    }
}

package com.kelompok.anemom.util

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

object DateFormats {
    private val indonesia = Locale("id", "ID")

    private val longDate = DateTimeFormatter.ofPattern("d MMMM yyyy", indonesia)
    private val shortDate = DateTimeFormatter.ofPattern("d MMM yyyy", indonesia)
    private val dayLongDate = DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", indonesia)

    fun long(date: LocalDate): String = date.format(longDate)
    fun short(date: LocalDate): String = date.format(shortDate)
    fun dayLong(date: LocalDate): String = date.format(dayLongDate)

    /** Angka Hb: 10.0 -> "10", 10.8 -> "10,8" (koma desimal Indonesia). */
    fun hb(value: Float): String =
        if (value % 1f == 0f) value.toInt().toString()
        else String.format(indonesia, "%.1f", value)
}

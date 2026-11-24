package com.kelompok.anemom.alarm

import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Format jam/tanggal alarm: "19.00", "19.00 WITA", "Selasa, 3 November 2026". */
object AlarmTimeText {
    private val indonesian = Locale.forLanguageTag("id-ID")
    private val dayLong = DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", indonesian)
    private val dateShort = DateTimeFormatter.ofPattern("d MMM yyyy", indonesian)

    fun time(t: LocalTime): String = String.format(indonesian, "%02d.%02d", t.hour, t.minute)

    fun timeWithZone(t: LocalTime): String = listOf(time(t), zoneLabel()).filter { it.isNotEmpty() }.joinToString(" ")

    fun dayLong(t: ZonedDateTime): String = t.format(dayLong)

    fun dateShort(t: ZonedDateTime): String = t.format(dateShort)

    /** WIB / WITA / WIT mengikuti zona waktu HP; kosong di luar Indonesia. */
    fun zoneLabel(zone: ZoneId = ZoneId.systemDefault()): String = when (zone.id) {
        "Asia/Jakarta", "Asia/Pontianak" -> "WIB"
        "Asia/Makassar", "Asia/Ujung_Pandang" -> "WITA"
        "Asia/Jayapura" -> "WIT"
        else -> ""
    }
}

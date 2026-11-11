package com.kelompok.anemom.data.local

import com.kelompok.anemom.data.model.HbResult
import com.kelompok.anemom.data.model.TtdEntry
import java.time.LocalDate
import java.time.LocalTime

/**
 * Pengubah data <-> teks untuk disimpan di DataStore Preferences.
 * Dipisah dari HealthStore supaya mudah diuji dengan unit test.
 */
object HealthCodec {

    private const val SEP = "|"

    // ---- Hasil Hb: satu baris per hasil -> "tanggal|nilai|fasilitas|sumber" ----

    fun encodeHb(results: List<HbResult>): String =
        results.joinToString("\n") { r ->
            listOf(r.date.toString(), r.value.toString(), clean(r.facility), clean(r.source))
                .joinToString(SEP)
        }

    fun decodeHb(raw: String): List<HbResult> =
        raw.lineSequence()
            .mapNotNull { line ->
                val parts = line.split(SEP)
                if (parts.size < 4) return@mapNotNull null
                val date = runCatching { LocalDate.parse(parts[0]) }.getOrNull() ?: return@mapNotNull null
                val value = parts[1].toFloatOrNull() ?: return@mapNotNull null
                HbResult(date = date, value = value, facility = parts[2], source = parts[3])
            }
            .sortedBy { it.date }
            .toList()

    // ---- Catatan TTD: satu string per hari -> "tanggal|jam" ----

    fun encodeTtd(entry: TtdEntry): String = "${entry.date}$SEP${entry.time}"

    fun decodeTtd(raw: String): TtdEntry? {
        val parts = raw.split(SEP)
        if (parts.size < 2) return null
        val date = runCatching { LocalDate.parse(parts[0]) }.getOrNull() ?: return null
        val time = runCatching { LocalTime.parse(parts[1]) }.getOrNull() ?: return null
        return TtdEntry(date, time)
    }

    private fun clean(text: String): String =
        text.replace(SEP, " ").replace("\n", " ").trim()
}

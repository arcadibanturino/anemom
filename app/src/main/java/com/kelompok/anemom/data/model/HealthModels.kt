package com.kelompok.anemom.data.model

import java.time.LocalDate
import java.time.LocalTime

/** Satu hasil tes Hb darah (dari laboratorium / fasilitas kesehatan). */
data class HbResult(
    val date: LocalDate,
    val value: Float,
    val facility: String,
    val source: String
)

/** Satu catatan minum TTD (maksimal satu per hari). */
data class TtdEntry(
    val date: LocalDate,
    val time: LocalTime
)

/**
 * Kategori risiko berdasarkan nilai Hb.
 *
 * Batas 11 g/dL mengikuti "Rujukan aplikasi" di desain. Pembagian risiko sedang/tinggi
 * di bawahnya adalah asumsi sementara.
 * TODO(klinis): konfirmasi batas per trimester dengan bidan/dosen pembimbing.
 */
enum class HbRisk(val label: String) {
    NORMAL("Normal"),
    MODERATE("Risiko sedang"),
    HIGH("Risiko tinggi");

    companion object {
        const val REFERENCE_HB = 11f

        fun classify(hb: Float): HbRisk = when {
            hb >= REFERENCE_HB -> NORMAL
            hb >= 10f -> MODERATE
            else -> HIGH
        }
    }
}

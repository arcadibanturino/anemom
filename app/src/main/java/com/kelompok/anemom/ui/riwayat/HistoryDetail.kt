package com.kelompok.anemom.ui.riwayat

import com.kelompok.anemom.data.model.HbRisk
import java.time.format.DateTimeFormatter

/** Isi Detail riwayat yang sama untuk layar dan PDF. */
data class HistoryDetail(
    val item: HistoryItem,
    /** Judul kartu utama, mis. "Hb darah terukur". */
    val headline: String,
    val badge: String,
    val value: String,
    val unit: String?,
    val status: HistoryChip?,
    val chipNote: String,
    /** (label, isi) */
    val rows: List<Pair<String, String>>,
    /** (judul, isi) */
    val advice: List<Pair<String, String>>,
    val footnote: String
)

private val longDate = DateTimeFormatter.ofPattern("d MMMM yyyy", indonesian)
private val longDateTime = DateTimeFormatter.ofPattern("d MMMM yyyy · HH.mm", indonesian)

fun HistoryItem.toDetail(): HistoryDetail = when (this) {
    is HistoryItem.Hb -> {
        val r = result
        val risk = r.risk ?: HbRisk.classify(r.value)
        HistoryDetail(
            item = this,
            headline = "Hb darah terukur",
            badge = "Laboratorium",
            value = hbText(r.value),
            unit = "g/dL",
            status = chip(),
            chipNote = "Status rujukan aplikasi; bukan penetapan diagnosis.",
            rows = listOfNotNull(
                "Tanggal pemeriksaan" to r.date.format(longDate),
                "Metode pemeriksaan" to "Tes Hb darah laboratorium",
                "Sumber data" to "${r.source.ifBlank { "Laboratorium" }} · Input manual Bunda",
                r.facility.takeIf { it.isNotBlank() }?.let { "Fasilitas kesehatan" to it },
                r.pregnancyWeek?.let { "Kondisi kehamilan" to "$it minggu · ${trimesterText(it)}" },
                r.notes?.let { "Catatan" to it }
            ),
            advice = if (risk == HbRisk.NORMAL) {
                listOf(
                    "Pertahankan kebiasaan baik" to "Lanjutkan TTD sesuai anjuran dan variasikan menu bergizi.",
                    "Periksa Hb sesuai jadwal" to "Ikuti jadwal pemeriksaan dari bidan atau dokter."
                )
            } else {
                listOf(
                    "Diskusikan hasil dengan bidan" to "Bawa hasil laboratorium ini saat kontrol berikutnya.",
                    "Lanjutkan TTD sesuai anjuran" to "Ikuti dosis TTD dari tenaga kesehatan. Jangan mengubah dosis sendiri."
                )
            },
            footnote = "Nilai ${hbText(r.value)} g/dL berasal dari laboratorium. Skor AI dan sinyal PPG " +
                "tidak digunakan sebagai pengganti nilai Hb ini."
        )
    }

    is HistoryItem.Scan -> {
        val s = screening
        val isPpg = kind == HistoryKind.PPG
        val anemia = s.label == "anemia"
        HistoryDetail(
            item = this,
            headline = if (isPpg) "Rekaman PPG" else "Skor AI Scan Mata",
            badge = if (isPpg) "Smartband" else "AI · penelitian",
            value = mainValue(),
            unit = null,
            status = chip(),
            chipNote = "Hasil skrining awal; bukan kadar Hb darah dan bukan diagnosis.",
            rows = listOfNotNull(
                "Tanggal dan waktu" to at.format(longDateTime),
                "Metode pemeriksaan" to if (isPpg) "Sinyal optik PPG dari smartband" else "Foto konjungtiva mata (U-Net + model warna)",
                s.scoreThreshold?.let { "Batas skrining" to "${(it * 100).toInt()}/100" },
                "Sumber data" to if (isPpg) "Smartband Anemom" else "AI Scan Mata · Aplikasi Anemom",
                s.pregnancyWeek?.let { "Kondisi kehamilan" to "$it minggu · ${trimesterText(it)}" }
            ),
            advice = listOfNotNull(
                if (anemia) {
                    "Cek Hb darah di laboratorium" to "Skor di atas batas skrining. Periksa Hb di puskesmas atau laboratorium."
                } else {
                    null
                },
                "Tetap periksa Hb sesuai jadwal" to "Skrining awal tidak menggantikan tes darah dan evaluasi bidan atau dokter.",
                "Lanjutkan TTD sesuai anjuran" to "Jangan mengubah dosis TTD berdasarkan hasil skrining."
            ),
            footnote = if (isPpg) {
                "Sinyal PPG adalah sinyal optik, bukan kadar Hb darah."
            } else {
                "Skor AI adalah peluang menurut model penelitian, bukan kadar Hb darah. Hasil dapat berbeda antar foto."
            }
        )
    }
}

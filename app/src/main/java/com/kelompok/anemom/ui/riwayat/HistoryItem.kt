package com.kelompok.anemom.ui.riwayat

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.RemoveRedEye
import androidx.compose.material.icons.outlined.Watch
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.kelompok.anemom.data.model.HbResult
import com.kelompok.anemom.data.model.HbRisk
import com.kelompok.anemom.data.repository.Screening
import com.kelompok.anemom.ui.theme.AmberBg
import com.kelompok.anemom.ui.theme.AmberText
import com.kelompok.anemom.ui.theme.ErrorRed
import com.kelompok.anemom.ui.theme.GreenBg
import com.kelompok.anemom.ui.theme.GreenText
import com.kelompok.anemom.ui.theme.MutedText
import com.kelompok.anemom.ui.theme.Pink100
import com.kelompok.anemom.ui.theme.RedBg
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Locale

/** Jenis pemeriksaan di Riwayat; juga dipakai sebagai argumen route detail. */
enum class HistoryKind(val route: String) {
    HB("hb"), AI("ai"), PPG("ppg");

    companion object {
        fun fromRoute(value: String?): HistoryKind? = entries.firstOrNull { it.route == value }
    }
}

/** Satu baris di Riwayat pemeriksaan: hasil Hb lab, AI Scan Mata, atau rekaman PPG. */
sealed interface HistoryItem {
    val kind: HistoryKind
    val id: Long
    /** Waktu lokal untuk mengurutkan dan mengelompokkan per bulan. Hb lab hanya punya tanggal. */
    val at: LocalDateTime

    data class Hb(val result: HbResult) : HistoryItem {
        override val kind = HistoryKind.HB
        override val id = result.id
        override val at: LocalDateTime = result.date.atStartOfDay()
    }

    data class Scan(val screening: Screening) : HistoryItem {
        override val kind = if (screening.method == "ppg_band") HistoryKind.PPG else HistoryKind.AI
        override val id = screening.id
        override val at: LocalDateTime = screening.measuredAt
            ?.let { runCatching { Instant.parse(it).atZone(ZoneId.systemDefault()).toLocalDateTime() }.getOrNull() }
            ?: LocalDate.MIN.atStartOfDay()
    }
}

/** Teks dan warna untuk kartu/detail riwayat. Hasil AI/PPG tidak diberi label penyakit. */
data class HistoryChip(val text: String, val container: Color, val content: Color)

internal val indonesian: Locale = Locale.forLanguageTag("id-ID")

internal fun HistoryItem.title(): String = when (this) {
    is HistoryItem.Hb -> "Hb laboratorium"
    is HistoryItem.Scan -> if (kind == HistoryKind.PPG) "Rekaman PPG" else "AI Scan Mata"
}

internal fun HistoryItem.icon(): ImageVector = when (kind) {
    HistoryKind.HB -> Icons.Outlined.WaterDrop
    HistoryKind.AI -> Icons.Outlined.RemoveRedEye
    HistoryKind.PPG -> Icons.Outlined.Watch
}

/** Angka utama: "10,8 g/dL", "Skor 68/100", atau status proses. */
internal fun HistoryItem.mainValue(): String = when (this) {
    is HistoryItem.Hb -> "${hbText(result.value)} g/dL"
    is HistoryItem.Scan -> when {
        screening.status == "queued" || screening.status == "processing" -> "Sedang diproses"
        screening.status == "failed" -> "Gagal diproses"
        screening.score != null -> "Skor ${(screening.score * 100).toInt()}/100"
        screening.estHb != null -> "Estimasi ${hbText(screening.estHb)} g/dL"
        else -> "Tercatat"
    }
}

internal fun HistoryItem.chip(): HistoryChip? = when (this) {
    is HistoryItem.Hb -> {
        val risk = result.risk ?: HbRisk.classify(result.value)
        val label = result.riskLabel ?: risk.label
        when (risk) {
            HbRisk.NORMAL -> HistoryChip(label, GreenBg, GreenText)
            HbRisk.MODERATE -> HistoryChip(label, AmberBg, AmberText)
            HbRisk.HIGH -> HistoryChip(label, RedBg, ErrorRed)
        }
    }
    is HistoryItem.Scan -> when (screening.label) {
        "anemia" -> HistoryChip("Disarankan cek Hb", AmberBg, AmberText)
        "non_anemia" -> HistoryChip("Tidak ada tanda", GreenBg, GreenText)
        else -> if (screening.status == "failed") null else HistoryChip("Diproses", Pink100, MutedText)
    }
}

internal fun HistoryItem.sourceLine(): String = when (this) {
    is HistoryItem.Hb -> listOf(
        "Sumber: ${result.source.ifBlank { "Laboratorium" }}",
        result.facility.ifBlank { null },
        "Hasil tes darah terukur."
    ).filterNotNull().joinToString(" · ")
    is HistoryItem.Scan -> if (kind == HistoryKind.PPG) {
        "Sumber: Smartband PPG · Sinyal optik, bukan tes Hb."
    } else {
        "Sumber: AI Scan Mata · Skor skrining AI, bukan kadar Hb darah."
    }
}

internal fun hbText(value: Float): String = String.format(indonesian, "%.1f", value)

internal fun trimesterText(week: Int): String = when {
    week <= 13 -> "Trimester pertama"
    week <= 27 -> "Trimester kedua"
    else -> "Trimester ketiga"
}

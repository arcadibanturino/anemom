package com.kelompok.anemom.data.repository

import com.kelompok.anemom.data.AppEvents
import com.kelompok.anemom.data.model.DashboardData
import com.kelompok.anemom.data.model.HbPoint
import com.kelompok.anemom.data.model.HbRisk
import com.kelompok.anemom.data.remote.ApiService
import com.kelompok.anemom.data.remote.apiCall
import com.kelompok.anemom.data.remote.dto.DashboardDto
import com.kelompok.anemom.util.DateFormats
import com.kelompok.anemom.util.PregnancyCalculator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import java.time.LocalDate
import kotlin.math.abs

/**
 * Sumber data halaman Beranda: satu panggilan GET /dashboard.
 * Usia kehamilan, trimester, HPL, kepatuhan TTD, Hb terakhir, dan tren dihitung server;
 * di sini hanya diubah menjadi teks untuk layar.
 */
class HomeRepository(
    private val api: ApiService,
    private val events: AppEvents
) {
    private val remote = RemoteSource(events) {
        apiCall { api.dashboard() }.map { it.toDashboardData() }
    }

    fun dashboard(): Flow<DashboardData> = remote.flow

    /** Pesan galat terakhir saat memuat Beranda (null jika berhasil). */
    val loadError: StateFlow<String?> = remote.error

    fun retry() = events.invalidateData()
}

private fun DashboardDto.toDashboardData(): DashboardData {
    val today = parseDate(today) ?: LocalDate.now()
    val firstName = motherName.trim().split(" ").firstOrNull().orEmpty()
    val dueDate = parseDate(pregnancy.dueDate)?.let { DateFormats.short(it) } ?: "-"

    val latest = lastHb
    val latestValue = latest?.hbValue?.toFloat()
    val latestDate = latest?.let { parseDate(it.testDate) }

    val trend = hbTrend.mapNotNull { point ->
        parseDate(point.date)?.let { HbPoint(DateFormats.dayMonth(it), point.hbValue.toFloat()) }
    }
    val trendNote = if (trend.size < 2) {
        "Tambahkan hasil Hb berikutnya untuk melihat tren."
    } else {
        val diff = trend.last().value - trend.first().value
        val amount = DateFormats.hb(abs(diff))
        when {
            diff > 0.04f -> "Hb naik $amount g/dL dari pemeriksaan pertama."
            diff < -0.04f -> "Hb turun $amount g/dL dari pemeriksaan pertama."
            else -> "Hb stabil dari pemeriksaan pertama."
        }
    }

    val reminderTime = ttd.reminderTime.replace(':', '.')

    return DashboardData(
        motherName = firstName,
        todayText = DateFormats.dayLong(today),
        pregnancyWeek = pregnancy.week,
        trimesterLabel = PregnancyCalculator.trimesterLabel(pregnancy.trimester),
        dueDateText = dueDate,
        lastHb = latestValue,
        lastHbNote = if (latestDate != null) {
            "${DateFormats.short(latestDate)} · Konsultasikan hasil dengan bidan."
        } else {
            "Belum ada hasil Hb. Tambahkan lewat Skrining > Input Hb darah."
        },
        risk = latest?.risk?.toRisk() ?: latestValue?.let { HbRisk.classify(it) },
        ttdPercent = ttd.percentLast7Days,
        ttdSummary = "${ttd.takenLast7Days} dari 7 hari · TTD berikutnya pukul $reminderTime",
        hbTrend = trend,
        hbTrendNote = trendNote
    )
}

private fun parseDate(value: String?): LocalDate? =
    value?.let { runCatching { LocalDate.parse(it) }.getOrNull() }

/** Kode risiko server (`normal`/`moderate`/`high`) -> enum aplikasi. */
private fun String.toRisk(): HbRisk? = when (this) {
    "normal" -> HbRisk.NORMAL
    "moderate" -> HbRisk.MODERATE
    "high" -> HbRisk.HIGH
    else -> null
}

package com.kelompok.anemom.data.repository

import com.kelompok.anemom.data.local.SessionManager
import com.kelompok.anemom.data.model.DashboardData
import com.kelompok.anemom.data.model.HbPoint
import com.kelompok.anemom.data.model.HbRisk
import com.kelompok.anemom.util.DateFormats
import com.kelompok.anemom.util.PregnancyCalculator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.LocalDate
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Sumber data halaman Beranda.
 *
 * - Nama, usia kehamilan, trimester, dan HPL dihitung dari akun lokal.
 * - Hb terakhir dan tren berasal dari hasil Hb yang diinput di menu Skrining > Input Hb darah.
 * - Kepatuhan TTD dihitung dari catatan minum TTD 7 hari terakhir.
 */
class HomeRepository(
    private val session: SessionManager,
    private val hbRepository: HbRepository,
    private val ttdRepository: TtdRepository
) {

    fun dashboard(): Flow<DashboardData> = combine(
        session.account,
        hbRepository.results,
        ttdRepository.entries
    ) { account, hbResults, ttdEntries ->
        val today = LocalDate.now()
        val hpht = account?.hpht?.takeIf { it.isNotBlank() }
            ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }

        val weeks = hpht?.let { PregnancyCalculator.weeksSince(it, today) } ?: 0
        val dueDate = hpht?.let { DateFormats.short(PregnancyCalculator.dueDate(it)) } ?: "-"
        val trimester = account?.trimester ?: 1
        val firstName = account?.name?.trim()?.split(" ")?.firstOrNull().orEmpty()

        // Hb
        val latest = hbResults.lastOrNull()
        val trendResults = hbResults.takeLast(5)
        val trendNote = when {
            trendResults.size < 2 -> "Tambahkan hasil Hb berikutnya untuk melihat tren."
            else -> {
                val diff = trendResults.last().value - trendResults.first().value
                val amount = DateFormats.hb(abs(diff))
                when {
                    diff > 0.04f -> "Hb naik $amount g/dL dari pemeriksaan pertama."
                    diff < -0.04f -> "Hb turun $amount g/dL dari pemeriksaan pertama."
                    else -> "Hb stabil dari pemeriksaan pertama."
                }
            }
        }

        // TTD: jumlah hari minum dalam 7 hari terakhir (termasuk hari ini)
        val windowStart = today.minusDays(6)
        val takenInWindow = ttdEntries.map { it.date }.distinct()
            .count { !it.isBefore(windowStart) && !it.isAfter(today) }
        val ttdPercent = (takenInWindow * 100f / 7f).roundToInt()

        DashboardData(
            motherName = firstName,
            todayText = DateFormats.dayLong(today),
            pregnancyWeek = weeks,
            trimesterLabel = PregnancyCalculator.trimesterLabel(trimester),
            dueDateText = dueDate,
            lastHb = latest?.value,
            lastHbNote = if (latest != null) {
                "${DateFormats.short(latest.date)} · Konsultasikan hasil dengan bidan."
            } else {
                "Belum ada hasil Hb. Tambahkan lewat Skrining > Input Hb darah."
            },
            risk = latest?.let { HbRisk.classify(it.value) },
            ttdPercent = ttdPercent,
            ttdSummary = "$takenInWindow dari 7 hari · TTD berikutnya pukul 20.00",
            hbTrend = trendResults.map { HbPoint(DateFormats.dayMonth(it.date), it.value) },
            hbTrendNote = trendNote
        )
    }
}

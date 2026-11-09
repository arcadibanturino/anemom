package com.kelompok.anemom.data.repository

import com.kelompok.anemom.data.local.SessionManager
import com.kelompok.anemom.data.model.DashboardData
import com.kelompok.anemom.data.model.HbPoint
import com.kelompok.anemom.util.DateFormats
import com.kelompok.anemom.util.PregnancyCalculator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

/**
 * Sumber data halaman Beranda.
 *
 * Nama, usia kehamilan, trimester, dan HPL dihitung dari akun lokal.
 * TODO(backend): Hb terakhir, kepatuhan TTD, dan tren Hb masih DATA CONTOH (dummy);
 * ganti dengan data dari API / Room.
 */
class HomeRepository(private val session: SessionManager) {

    fun dashboard(): Flow<DashboardData> = session.account.map { account ->
        val today = LocalDate.now()
        val hpht = account?.hpht?.takeIf { it.isNotBlank() }?.let { runCatching { LocalDate.parse(it) }.getOrNull() }

        val weeks = hpht?.let { PregnancyCalculator.weeksSince(it, today) } ?: 0
        val dueDate = hpht?.let { DateFormats.short(PregnancyCalculator.dueDate(it)) } ?: "-"
        val trimester = account?.trimester ?: 1
        val firstName = account?.name?.trim()?.split(" ")?.firstOrNull().orEmpty()

        DashboardData(
            motherName = firstName,
            todayText = DateFormats.dayLong(today),
            pregnancyWeek = weeks,
            trimesterLabel = PregnancyCalculator.trimesterLabel(trimester),
            dueDateText = dueDate,
            // ---- data contoh ----
            lastHb = 10.8f,
            lastHbNote = "3 Nov 2026 · Konsultasikan hasil dengan bidan.",
            riskLabel = "Risiko sedang",
            ttdPercent = 86,
            ttdSummary = "6 dari 7 hari · TTD berikutnya pukul 20.00",
            hbTrend = listOf(
                HbPoint("8 Sep", 10f),
                HbPoint("22 Sep", 10.5f),
                HbPoint("6 Okt", 10.8f),
                HbPoint("20 Okt", 11.2f),
                HbPoint("3 Nov", 11f)
            ),
            hbTrendNote = "Hb naik 0,5 g/dL dari pemeriksaan pertama."
        )
    }
}

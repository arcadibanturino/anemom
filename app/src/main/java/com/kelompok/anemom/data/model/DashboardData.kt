package com.kelompok.anemom.data.model

data class HbPoint(
    val label: String,
    val value: Float
)

/** Data yang ditampilkan di halaman Beranda. */
data class DashboardData(
    val motherName: String,
    val todayText: String,
    val pregnancyWeek: Int,
    val trimesterLabel: String,
    val dueDateText: String,
    /** Hb laboratorium terakhir; null jika belum ada hasil yang disimpan. */
    val lastHb: Float?,
    val lastHbNote: String,
    val risk: HbRisk?,
    val ttdPercent: Int,
    val ttdSummary: String,
    val hbTrend: List<HbPoint>,
    val hbTrendNote: String
)

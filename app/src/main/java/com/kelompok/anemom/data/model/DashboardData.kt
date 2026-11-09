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
    val lastHb: Float,
    val lastHbNote: String,
    val riskLabel: String,
    val ttdPercent: Int,
    val ttdSummary: String,
    val hbTrend: List<HbPoint>,
    val hbTrendNote: String
)

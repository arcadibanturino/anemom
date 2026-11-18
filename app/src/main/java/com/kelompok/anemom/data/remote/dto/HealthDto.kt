package com.kelompok.anemom.data.remote.dto

import com.google.gson.annotations.SerializedName

// ---- Hb laboratorium ----

data class HbResultDto(
    val id: Long = 0,
    @SerializedName("test_date") val testDate: String = "",
    @SerializedName("hb_value") val hbValue: Double = 0.0,
    /** `normal` | `moderate` | `high` (dihitung server). */
    val risk: String? = null,
    val facility: String? = null,
    val source: String? = null
)

data class HbCreateRequest(
    @SerializedName("hb_value") val hbValue: Float,
    @SerializedName("test_date") val testDate: String,
    val facility: String? = null,
    val source: String? = null,
    /** Dikirim ulang dengan UUID yang sama = server tidak membuat duplikat. */
    @SerializedName("client_uuid") val clientUuid: String? = null
)

// ---- TTD ----

data class TtdDayDto(
    val date: String = "",
    /** "HH:mm" */
    val time: String = "00:00"
)

data class TtdMonthDto(
    val month: String = "",
    val days: List<TtdDayDto> = emptyList(),
    @SerializedName("taken_today") val takenToday: Boolean = false,
    @SerializedName("taken_last_7_days") val takenLast7Days: Int = 0,
    @SerializedName("percent_last_7_days") val percentLast7Days: Int = 0
)

/** Kosong = server memakai tanggal dan jam saat ini. */
data class TtdTakeRequest(
    val date: String? = null,
    val time: String? = null
)

data class TtdTakeDto(
    val date: String = "",
    val time: String = "",
    val created: Boolean = false
)

data class TtdSettingsDto(
    @SerializedName("reminder_on") val reminderOn: Boolean = true,
    /** "HH:mm" */
    @SerializedName("reminder_time") val reminderTime: String = "20:00"
)

data class TtdSettingsRequest(
    @SerializedName("reminder_on") val reminderOn: Boolean? = null,
    @SerializedName("reminder_time") val reminderTime: String? = null
)

// ---- Beranda ----

data class HbTrendPointDto(
    val date: String = "",
    @SerializedName("hb_value") val hbValue: Double = 0.0
)

data class TtdSummaryDto(
    @SerializedName("taken_today") val takenToday: Boolean = false,
    @SerializedName("taken_last_7_days") val takenLast7Days: Int = 0,
    @SerializedName("percent_last_7_days") val percentLast7Days: Int = 0,
    @SerializedName("reminder_on") val reminderOn: Boolean = true,
    @SerializedName("reminder_time") val reminderTime: String = "20:00"
)

data class DashboardDto(
    @SerializedName("mother_name") val motherName: String = "",
    val today: String? = null,
    val pregnancy: PregnancyDto = PregnancyDto(),
    @SerializedName("last_hb") val lastHb: HbResultDto? = null,
    @SerializedName("hb_trend") val hbTrend: List<HbTrendPointDto> = emptyList(),
    val ttd: TtdSummaryDto = TtdSummaryDto()
)

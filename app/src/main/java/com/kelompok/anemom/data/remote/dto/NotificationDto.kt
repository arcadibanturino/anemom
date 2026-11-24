package com.kelompok.anemom.data.remote.dto

import com.google.gson.annotations.SerializedName

/** Satu notifikasi dari GET /notifications. Waktu dalam ISO-8601 UTC (mis. 2026-11-25T00:30:00Z). */
data class NotificationDto(
    val id: Long = 0,
    /** `ttd_reminder` | `ttd_done` | `hb_reminder` | `checkup` | `info` */
    val type: String? = null,
    val title: String = "",
    val body: String = "",
    @SerializedName("scheduled_at") val scheduledAt: String? = null,
    @SerializedName("read_at") val readAt: String? = null
)

/** GET /notifications/unread-count */
data class UnreadCountDto(
    val unread: Int = 0
)

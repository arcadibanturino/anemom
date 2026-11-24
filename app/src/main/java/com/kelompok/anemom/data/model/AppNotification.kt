package com.kelompok.anemom.data.model

import java.time.Instant

/** Jenis notifikasi; menentukan ikon dan tombol aksi di layar Notifikasi. */
enum class NotificationType {
    TTD_REMINDER,
    TTD_DONE,
    HB_REMINDER,
    CHECKUP,
    INFO;

    companion object {
        fun fromCode(code: String?): NotificationType = when (code) {
            "ttd_reminder" -> TTD_REMINDER
            "ttd_done" -> TTD_DONE
            "hb_reminder" -> HB_REMINDER
            "checkup" -> CHECKUP
            else -> INFO
        }
    }
}

data class AppNotification(
    val id: Long,
    val type: NotificationType,
    val title: String,
    val body: String,
    /** Waktu notifikasi; null bila server tidak mengirim. */
    val time: Instant?,
    val isRead: Boolean
)

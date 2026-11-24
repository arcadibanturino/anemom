package com.kelompok.anemom.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * - ACTION_FIRE: jam alarm tiba -> bunyikan alarm, lalu pasang alarm berikutnya.
 * - ACTION_SNOOZE_10: tombol "Tunda 10 menit" di notifikasi.
 * - Sistem: HP menyala ulang, jam/zona waktu diubah, aplikasi diperbarui -> pasang ulang alarm.
 */
class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val scheduler = TtdAlarmScheduler(context)
        when (intent.action) {
            ACTION_FIRE -> {
                val test = intent.getBooleanExtra(EXTRA_TEST, false)
                if (test) {
                    AlarmNotifier.show(context, test = true)
                    return
                }
                if (!scheduler.isEnabled || !scheduler.reminderOn) return
                // TTD hari ini sudah dicatat: tidak perlu berbunyi.
                if (!scheduler.isTakenToday()) AlarmNotifier.show(context)
                scheduler.onFired()
            }

            ACTION_SNOOZE_10 -> scheduler.snooze(10)

            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED -> {
                scheduler.restore()
                if (intent.action == Intent.ACTION_BOOT_COMPLETED && scheduler.missedToday()) {
                    AlarmNotifier.show(context, missed = true)
                }
            }
        }
    }

    companion object {
        const val ACTION_FIRE = "com.kelompok.anemom.alarm.FIRE"
        const val ACTION_SNOOZE_10 = "com.kelompok.anemom.alarm.SNOOZE_10"
        const val EXTRA_TEST = "test"
    }
}

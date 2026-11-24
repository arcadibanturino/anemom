package com.kelompok.anemom.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.kelompok.anemom.MainActivity
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Alarm TTD harian yang tetap berbunyi walau aplikasi ditutup atau layar terkunci.
 *
 * Memakai AlarmManager.setAlarmClock (jalur yang sama dengan aplikasi Jam): tepat waktu, tidak ditunda
 * mode hemat daya (Doze), dan menampilkan ikon alarm di status bar. Hanya SATU alarm yang terpasang:
 * mana yang lebih dulu antara tunda (snooze) dan jadwal harian. Setelah berbunyi, AlarmReceiver
 * memasang alarm berikutnya.
 *
 * Batas Android: alarm hilang bila aplikasi di-"Paksa berhenti" dari Pengaturan, dan tidak bisa berbunyi
 * saat HP benar-benar dimatikan (setelah HP menyala, BootReceiver memasang ulang alarm).
 */
class TtdAlarmScheduler(context: Context) {

    private val context = context.applicationContext
    private val prefs = this.context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val alarmManager = this.context.getSystemService(AlarmManager::class.java)

    /** true setelah login dan jam alarm diketahui; false setelah logout. */
    val isEnabled: Boolean get() = prefs.getBoolean(KEY_ENABLED, false)

    /** Pengingat dinyalakan Bunda (bawaan menyala). Bila Hb rendah, server memaksanya menyala. */
    val reminderOn: Boolean get() = prefs.getBoolean(KEY_ON, true)

    val reminderTime: LocalTime
        get() = runCatching { LocalTime.parse(prefs.getString(KEY_TIME, null)) }.getOrDefault(DEFAULT_TIME)

    /** Tanggal terakhir TTD tercatat "Sudah" di HP ini; alarm hari itu tidak perlu berbunyi lagi. */
    private val takenDate: LocalDate?
        get() = prefs.getString(KEY_TAKEN_DATE, null)?.let { runCatching { LocalDate.parse(it) }.getOrNull() }

    private val snoozeAt: Long get() = prefs.getLong(KEY_SNOOZE_AT, 0L)

    /** Simpan pengaturan dari server dan pasang (atau lepas) alarm. */
    fun update(time: LocalTime, on: Boolean = reminderOn) {
        prefs.edit()
            .putString(KEY_TIME, time.withSecond(0).withNano(0).toString())
            .putBoolean(KEY_ON, on)
            .putBoolean(KEY_ENABLED, true)
            .apply()
        if (!on) AlarmNotifier.cancel(context)
        scheduleNext()
    }

    /** Pasang ulang dengan jam yang tersimpan (mis. setelah HP menyala ulang). */
    fun restore() {
        if (isEnabled) scheduleNext() else cancelAlarm()
    }

    /** Logout: semua alarm dibatalkan. */
    fun disable() {
        prefs.edit().clear().apply()
        cancelAlarm()
        alarmManager.cancel(testPendingIntent())
        AlarmNotifier.cancel(context)
    }

    fun markTaken(date: LocalDate = LocalDate.now()) {
        prefs.edit().putString(KEY_TAKEN_DATE, date.toString()).putLong(KEY_SNOOZE_AT, 0L).apply()
        AlarmNotifier.cancel(context)
        scheduleNext()
    }

    fun isTakenToday(): Boolean = takenDate == LocalDate.now()

    /** Tunda [minutes] menit dari sekarang. Tidak mencatat konsumsi. Mengembalikan waktu alarm berikutnya. */
    fun snooze(minutes: Int): ZonedDateTime {
        val at = ZonedDateTime.now().plusMinutes(minutes.toLong()).withSecond(0).withNano(0)
        prefs.edit().putLong(KEY_SNOOZE_AT, at.toInstant().toEpochMilli()).apply()
        AlarmNotifier.cancel(context)
        scheduleNext()
        return at
    }

    /** Dipanggil AlarmReceiver saat alarm (harian atau tunda) berbunyi. */
    fun onFired() {
        if (snoozeAt != 0L && snoozeAt <= System.currentTimeMillis() + 60_000) {
            prefs.edit().putLong(KEY_SNOOZE_AT, 0L).apply()
        }
        scheduleNext()
    }

    /** Alarm uji untuk demo: berbunyi [seconds] detik lagi, tidak mengubah jadwal harian. */
    fun scheduleTest(seconds: Long = 60): ZonedDateTime {
        val at = ZonedDateTime.now().plusSeconds(seconds)
        setExact(at.toInstant().toEpochMilli(), testPendingIntent())
        return at
    }

    /** Waktu alarm berikutnya (tunda atau harian); null bila tidak aktif. */
    fun nextTrigger(): ZonedDateTime? {
        if (!isEnabled || !reminderOn) return null
        val zone = ZoneId.systemDefault()
        val now = ZonedDateTime.now(zone)
        var daily = now.toLocalDate().atTime(reminderTime).atZone(zone)
        if (!daily.isAfter(now) || takenDate == now.toLocalDate()) daily = daily.plusDays(1)
        val snooze = snoozeAt.takeIf { it > System.currentTimeMillis() }
            ?.let { ZonedDateTime.ofInstant(Instant.ofEpochMilli(it), zone) }
        return if (snooze != null && snooze.isBefore(daily)) snooze else daily
    }

    /** Alarm hari ini sudah lewat (dalam [withinHours] jam) tetapi TTD belum tercatat: dipakai setelah HP menyala ulang. */
    fun missedToday(withinHours: Long = 3): Boolean {
        if (!isEnabled || !reminderOn || isTakenToday()) return false
        val now = ZonedDateTime.now()
        val today = now.toLocalDate().atTime(reminderTime).atZone(now.zone)
        return today.isBefore(now) && today.isAfter(now.minusHours(withinHours))
    }

    fun canScheduleExact(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()

    private fun scheduleNext() {
        val next = nextTrigger() ?: return cancelAlarm()
        setExact(next.toInstant().toEpochMilli(), alarmPendingIntent())
    }

    private fun setExact(triggerAtMillis: Long, operation: PendingIntent) {
        if (canScheduleExact()) {
            // Ketuk ikon alarm di status bar -> buka aplikasi.
            val show = PendingIntent.getActivity(
                context, REQ_SHOW, Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            alarmManager.setAlarmClock(AlarmManager.AlarmClockInfo(triggerAtMillis, show), operation)
        } else {
            // Izin alarm tepat waktu dicabut: tetap berbunyi, tetapi bisa terlambat beberapa menit.
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, operation)
        }
    }

    private fun cancelAlarm() {
        alarmManager.cancel(alarmPendingIntent())
    }

    private fun alarmPendingIntent(): PendingIntent = PendingIntent.getBroadcast(
        context, REQ_ALARM,
        Intent(context, AlarmReceiver::class.java).setAction(AlarmReceiver.ACTION_FIRE),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )

    private fun testPendingIntent(): PendingIntent = PendingIntent.getBroadcast(
        context, REQ_TEST,
        Intent(context, AlarmReceiver::class.java)
            .setAction(AlarmReceiver.ACTION_FIRE)
            .putExtra(AlarmReceiver.EXTRA_TEST, true),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )

    companion object {
        /** Bila server belum pernah terbaca. */
        val DEFAULT_TIME: LocalTime = LocalTime.of(19, 0)

        private const val PREFS = "ttd_alarm"
        private const val KEY_ENABLED = "enabled"
        private const val KEY_ON = "reminder_on"
        private const val KEY_TIME = "time"
        private const val KEY_TAKEN_DATE = "taken_date"
        private const val KEY_SNOOZE_AT = "snooze_at"

        private const val REQ_ALARM = 1001
        private const val REQ_TEST = 1002
        private const val REQ_SHOW = 1003
    }
}

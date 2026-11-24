package com.kelompok.anemom.alarm

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.kelompok.anemom.R

/**
 * Notifikasi alarm TTD: berbunyi dan bergetar berulang (FLAG_INSISTENT) sampai Bunda menanggapi,
 * dan membuka AlarmActivity layar penuh di atas layar kunci (full-screen intent).
 * Bila HP sedang dipakai, Android menampilkannya sebagai notifikasi melayang; ketuk untuk membuka.
 */
object AlarmNotifier {

    /** Pengaturan suara channel tidak bisa diubah setelah dibuat; ganti ID bila suaranya diubah. */
    const val CHANNEL_ID = "ttd_alarm_v1"
    private const val NOTIFICATION_ID = 2001

    /** Alarm berhenti sendiri bila tidak ditanggapi selama ini. */
    private const val RING_TIMEOUT_MS = 10 * 60 * 1000L

    fun ensureChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        val sound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val channel = NotificationChannel(CHANNEL_ID, "Alarm TTD", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Alarm harian minum Tablet Tambah Darah"
            setSound(
                sound,
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 800, 600, 800, 600)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        manager.createNotificationChannel(channel)
    }

    /**
     * @param test alarm uji dari Pengaturan pengingat.
     * @param missed alarm terlewat (HP mati saat jadwal): tampil tanpa nada berulang.
     */
    fun show(context: Context, test: Boolean = false, missed: Boolean = false) {
        ensureChannel(context)
        val fullScreen = PendingIntent.getActivity(
            context, 0,
            AlarmActivity.newIntent(context, test = test),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val snooze = PendingIntent.getBroadcast(
            context, 1,
            Intent(context, AlarmReceiver::class.java).setAction(AlarmReceiver.ACTION_SNOOZE_10),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val title = when {
            missed -> "Pengingat TTD terlewat"
            test -> "Uji alarm TTD"
            else -> "Waktunya minum TTD, Bunda"
        }
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notif_ttd)
            .setContentTitle(title)
            .setContentText("Geser untuk mengonfirmasi: sudah atau belum diminum.")
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(fullScreen)
            .setFullScreenIntent(fullScreen, true)
            .addAction(0, "Tunda 10 menit", snooze)
            .setOngoing(!missed)
            .setAutoCancel(false)
            .setTimeoutAfter(RING_TIMEOUT_MS)
            .build()
        if (!missed) notification.flags = notification.flags or Notification.FLAG_INSISTENT

        // Tanpa izin notifikasi (Android 13+) alarm tidak bisa tampil; Pengaturan pengingat memberi tahu Bunda.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }

    /** Hentikan nada dan tutup notifikasi alarm. */
    fun cancel(context: Context) {
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
    }
}

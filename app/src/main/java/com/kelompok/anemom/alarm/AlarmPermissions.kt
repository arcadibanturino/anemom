package com.kelompok.anemom.alarm

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

/** Status izin Android yang dibutuhkan alarm, dan Intent untuk membuka pengaturannya. */
data class AlarmPermissionStatus(
    /** Notifikasi diizinkan (Android 13+ perlu izin; juga bisa dimatikan manual). */
    val notifications: Boolean,
    /** Alarm tepat waktu (Android 12+). */
    val exactAlarm: Boolean,
    /** Tampilan layar penuh di atas layar kunci (Android 14+). */
    val fullScreen: Boolean,
    /** Aplikasi dikecualikan dari optimasi baterai (disarankan untuk HP Xiaomi/Oppo/Vivo/Realme). */
    val batteryUnrestricted: Boolean
) {
    val allRequired: Boolean get() = notifications && exactAlarm && fullScreen
}

object AlarmPermissions {

    fun status(context: Context): AlarmPermissionStatus {
        val notificationsGranted = (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED) && NotificationManagerCompat.from(context).areNotificationsEnabled()
        val exact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()
        val fullScreen = Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE ||
            context.getSystemService(NotificationManager::class.java).canUseFullScreenIntent()
        val battery = context.getSystemService(PowerManager::class.java)
            .isIgnoringBatteryOptimizations(context.packageName)
        return AlarmPermissionStatus(notificationsGranted, exact, fullScreen, battery)
    }

    /** Halaman pengaturan notifikasi aplikasi. */
    fun notificationSettings(context: Context): Intent =
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)

    fun exactAlarmSettings(context: Context): Intent =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}"))
        } else {
            appDetails(context)
        }

    fun fullScreenSettings(context: Context): Intent =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, Uri.parse("package:${context.packageName}"))
        } else {
            appDetails(context)
        }

    /** Daftar optimasi baterai (tanpa izin khusus); Bunda memilih Anemom -> "Tidak dioptimalkan". */
    fun batterySettings(): Intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)

    fun appDetails(context: Context): Intent =
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))
}

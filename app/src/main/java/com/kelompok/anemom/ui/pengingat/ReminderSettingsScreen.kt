package com.kelompok.anemom.ui.pengingat

import android.Manifest
import android.content.Intent
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kelompok.anemom.alarm.AlarmActivity
import com.kelompok.anemom.alarm.AlarmPermissions
import com.kelompok.anemom.alarm.AlarmTimeText
import com.kelompok.anemom.ui.AppViewModelProvider
import com.kelompok.anemom.ui.components.AnemomCard
import com.kelompok.anemom.ui.components.AnemomOutlinedButton
import com.kelompok.anemom.ui.components.AnemomScreen
import com.kelompok.anemom.ui.components.GradientButton
import com.kelompok.anemom.ui.theme.AmberText
import com.kelompok.anemom.ui.theme.GreenText
import com.kelompok.anemom.ui.theme.Maroon
import com.kelompok.anemom.ui.theme.MutedText
import com.kelompok.anemom.ui.theme.Pink100
import com.kelompok.anemom.ui.theme.Pink500

/** Pengaturan pengingat (dari Profil): jam alarm TTD, izin Android, pratinjau dan uji alarm. */
@Composable
fun ReminderSettingsScreen(
    onBack: () -> Unit,
    onEditTime: () -> Unit,
    viewModel: ReminderViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var permissions by remember { mutableStateOf(AlarmPermissions.status(context)) }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        permissions = AlarmPermissions.status(context)
        viewModel.refresh()
    }
    LaunchedEffect(state.message) {
        state.message?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.messageShown()
        }
    }

    // Android hanya menampilkan dialog izin sekali-dua kali; setelah itu arahkan ke pengaturan notifikasi.
    var notificationAsked by rememberSaveable { mutableStateOf(false) }
    val notificationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        notificationAsked = true
        permissions = AlarmPermissions.status(context)
    }
    fun open(intent: Intent) = runCatching { context.startActivity(intent) }
        .onFailure { context.startActivity(AlarmPermissions.appDetails(context)) }

    AnemomScreen(title = "Pengaturan pengingat", subtitle = "Alarm TTD setiap hari", onBack = onBack) {
        Text(
            text = "Pengingat Tablet Tambah Darah",
            style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp),
            fontWeight = FontWeight.Bold,
            color = Maroon
        )
        Spacer(Modifier.height(12.dp))

        AnemomCard(Modifier.fillMaxWidth()) {
            Text(if (state.reminderOn) "Alarm TTD · Setiap hari" else "Alarm TTD · Nonaktif", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Maroon)
            Spacer(Modifier.height(6.dp))
            Text(
                text = AlarmTimeText.timeWithZone(state.reminderTime),
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold,
                color = if (state.reminderOn) Pink500 else MutedText
            )
            Text("Setelah makan · Ikuti dosis tenaga kesehatan.", style = MaterialTheme.typography.bodySmall, color = MutedText)
            state.nextAlarm?.let {
                Text(
                    text = "Alarm berikutnya: ${AlarmTimeText.dayLong(it)} · ${AlarmTimeText.time(it.toLocalTime())}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MutedText
                )
            }
            Spacer(Modifier.height(14.dp))
            GradientButton(text = "Ubah jam alarm", onClick = onEditTime, modifier = Modifier.fillMaxWidth())
        }

        Spacer(Modifier.height(14.dp))
        AnemomCard(Modifier.fillMaxWidth(), containerColor = Pink100.copy(alpha = 0.75f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Pengingat TTD", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Maroon)
                    Text(
                        text = when {
                            !state.canDisable -> "Wajib aktif · Hb terakhir di bawah normal"
                            state.reminderOn -> "Aktif · alarm berbunyi setiap hari"
                            else -> "Nonaktif · alarm tidak berbunyi"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MutedText
                    )
                }
                Switch(
                    checked = state.reminderOn,
                    onCheckedChange = viewModel::setReminderOn,
                    enabled = state.canDisable && !state.isToggling,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Pink500,
                        disabledCheckedThumbColor = Color.White,
                        disabledCheckedTrackColor = Pink500.copy(alpha = 0.55f),
                        uncheckedThumbColor = MutedText,
                        uncheckedTrackColor = Color.White
                    )
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = if (state.canDisable) {
                    "Bawaan menyala. Bunda boleh mematikannya, tetapi pengingat membantu minum TTD tepat waktu."
                } else {
                    "Hasil Hb laboratorium terakhir di bawah normal, jadi minum TTD wajib dan pengingat tidak bisa " +
                        "dimatikan. Pengingat bisa dimatikan lagi setelah hasil Hb berikutnya normal."
                },
                style = MaterialTheme.typography.bodySmall,
                color = Maroon
            )
        }

        Spacer(Modifier.height(14.dp))
        AnemomCard(Modifier.fillMaxWidth()) {
            Text("Izin notifikasi & alarm Android", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Maroon)
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Alarm butuh izin notifikasi, alarm tepat waktu, dan tampilan layar penuh. " +
                    "Izin diberikan atau dicabut melalui Pengaturan Android.",
                style = MaterialTheme.typography.bodySmall,
                color = MutedText
            )
            Spacer(Modifier.height(10.dp))
            PermissionRow("Notifikasi", permissions.notifications, required = true) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !notificationAsked) {
                    notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    open(AlarmPermissions.notificationSettings(context))
                }
            }
            PermissionRow("Alarm tepat waktu", permissions.exactAlarm, required = true) {
                open(AlarmPermissions.exactAlarmSettings(context))
            }
            PermissionRow("Layar penuh saat terkunci", permissions.fullScreen, required = true) {
                open(AlarmPermissions.fullScreenSettings(context))
            }
            PermissionRow("Baterai tanpa batasan (disarankan)", permissions.batteryUnrestricted, required = false) {
                open(AlarmPermissions.batterySettings())
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Jika izin ditolak, alarm tampil sebagai notifikasi biasa atau tidak muncul. " +
                    "Pada HP Xiaomi, Oppo, Vivo, atau Realme, izinkan juga \"Mulai otomatis\" untuk Anemom.",
                style = MaterialTheme.typography.labelSmall,
                color = MutedText
            )
        }

        Spacer(Modifier.height(16.dp))
        GradientButton(
            text = "Pratinjau alarm layar penuh",
            onClick = { context.startActivity(AlarmActivity.newIntent(context, preview = true)) },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(10.dp))
        AnemomOutlinedButton(
            text = "Uji alarm 1 menit lagi",
            onClick = viewModel::scheduleTest,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "Pratinjau tidak berbunyi. Uji alarm berbunyi sungguhan; kunci layar atau tutup aplikasi untuk mencobanya. " +
                "Keduanya tidak mencatat konsumsi.",
            style = MaterialTheme.typography.labelSmall,
            color = MutedText
        )
    }
}

@Composable
private fun PermissionRow(label: String, granted: Boolean, required: Boolean, onFix: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(enabled = !granted, onClick = onFix)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(if (granted) GreenText else if (required) Pink500 else AmberText)
        )
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyMedium, color = Maroon)
            Text(
                text = if (granted) "Diizinkan" else if (required) "Belum diizinkan" else "Belum diatur",
                style = MaterialTheme.typography.labelSmall,
                color = if (granted) GreenText else MutedText
            )
        }
        if (!granted) {
            Text("Izinkan", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = Pink500)
        }
    }
}

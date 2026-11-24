package com.kelompok.anemom.ui.pengingat

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kelompok.anemom.alarm.AlarmTimeText
import com.kelompok.anemom.ui.AppViewModelProvider
import com.kelompok.anemom.ui.components.AnemomCard
import com.kelompok.anemom.ui.components.AnemomScreen
import com.kelompok.anemom.ui.components.GradientButton
import com.kelompok.anemom.ui.theme.CardBorder
import com.kelompok.anemom.ui.theme.Maroon
import com.kelompok.anemom.ui.theme.MutedText
import com.kelompok.anemom.ui.theme.Pink100
import com.kelompok.anemom.ui.theme.Pink500
import java.time.LocalTime

private val presets = listOf(LocalTime.of(18, 0), LocalTime.of(19, 0), LocalTime.of(20, 0))

/** Ubah jam alarm TTD harian. Disimpan ke server, lalu alarm di HP dipasang ulang. */
@Composable
fun AlarmTimeScreen(
    onBack: () -> Unit,
    viewModel: ReminderViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var hour by rememberSaveable { mutableStateOf(state.reminderTime.hour) }
    var minute by rememberSaveable { mutableStateOf(state.reminderTime.minute) }
    var touched by rememberSaveable { mutableStateOf(false) }

    // Jam dari server datang belakangan: ikuti selama Bunda belum mengubah apa pun.
    LaunchedEffect(state.reminderTime) {
        if (!touched) {
            hour = state.reminderTime.hour
            minute = state.reminderTime.minute
        }
    }

    LaunchedEffect(state.message) {
        state.message?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.messageShown()
        }
    }
    LaunchedEffect(state.saved) { if (state.saved) onBack() }

    AnemomScreen(title = "Ubah jam alarm TTD", subtitle = "Jadwal setiap hari · ${AlarmTimeText.zoneLabel()}", onBack = onBack) {
        Spacer(Modifier.height(8.dp))
        AnemomCard(Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Stepper(
                    value = hour,
                    label = "Jam",
                    onUp = { touched = true; hour = (hour + 1) % 24 },
                    onDown = { touched = true; hour = (hour + 23) % 24 }
                )
                Text(".", fontSize = 56.sp, fontWeight = FontWeight.Bold, color = Pink500, modifier = Modifier.padding(horizontal = 6.dp))
                Stepper(
                    value = minute,
                    label = "Menit",
                    onUp = { touched = true; minute = (minute / 5 * 5 + 5) % 60 },
                    onDown = { touched = true; minute = ((minute + 4) / 5 * 5 + 55) % 60 }
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = "Pilihan cepat · minum setelah makan malam",
                style = MaterialTheme.typography.bodySmall,
                color = MutedText
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                presets.forEach { t ->
                    val selected = t.hour == hour && t.minute == minute
                    Text(
                        text = AlarmTimeText.time(t),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (selected) Color.White else Maroon,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(if (selected) Pink500 else Pink100)
                            .border(1.dp, if (selected) Pink500 else CardBorder, RoundedCornerShape(50))
                            .clickable {
                                touched = true
                                hour = t.hour
                                minute = t.minute
                            }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(20.dp))
        GradientButton(
            text = "Simpan jam alarm",
            onClick = { viewModel.saveTime(LocalTime.of(hour, minute)) },
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.isSaving,
            isLoading = state.isSaving
        )

        Spacer(Modifier.height(20.dp))
        AnemomCard(Modifier.fillMaxWidth(), containerColor = Pink100.copy(alpha = 0.75f)) {
            Text("Izin tetap mengikuti Android", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Maroon)
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Bawaan 19.00. Pengingat dinyalakan atau dimatikan di Pengaturan pengingat; " +
                    "wajib aktif bila Hb terakhir di bawah normal. Hak akses notifikasi dan alarm dikelola dari Pengaturan Android.",
                style = MaterialTheme.typography.bodySmall,
                color = Maroon
            )
        }
    }
}

@Composable
private fun Stepper(value: Int, label: String, onUp: () -> Unit, onDown: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(onClick = onUp) {
            Icon(Icons.Filled.KeyboardArrowUp, contentDescription = "$label tambah", tint = Pink500)
        }
        Text(
            text = "%02d".format(value),
            fontSize = 56.sp,
            fontWeight = FontWeight.Bold,
            color = Pink500,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(92.dp)
        )
        IconButton(onClick = onDown) {
            Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "$label kurang", tint = Pink500)
        }
        Text(label, style = MaterialTheme.typography.bodySmall, color = MutedText)
    }
}

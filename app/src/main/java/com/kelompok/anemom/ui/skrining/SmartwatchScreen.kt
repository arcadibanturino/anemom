package com.kelompok.anemom.ui.skrining

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MonitorHeart
import androidx.compose.material.icons.outlined.Watch
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kelompok.anemom.ui.AppViewModelProvider
import com.kelompok.anemom.ui.components.AnemomCard
import com.kelompok.anemom.ui.components.AnemomChip
import com.kelompok.anemom.ui.components.AnemomOutlinedButton
import com.kelompok.anemom.ui.components.AnemomScreen
import com.kelompok.anemom.ui.components.GradientButton
import com.kelompok.anemom.ui.components.NoteCard
import com.kelompok.anemom.ui.theme.AmberBg
import com.kelompok.anemom.ui.theme.AmberText
import com.kelompok.anemom.ui.theme.GreenBg
import com.kelompok.anemom.ui.theme.GreenText
import com.kelompok.anemom.ui.theme.Maroon
import com.kelompok.anemom.ui.theme.MutedText
import com.kelompok.anemom.ui.theme.Pink100
import com.kelompok.anemom.ui.theme.Pink500

/** Smartwatch & PPG. Seluruh data di layar ini SIMULASI (lihat [SmartwatchViewModel]). */
@Composable
fun SmartwatchScreen(
    onBack: () -> Unit,
    onInputHb: () -> Unit,
    viewModel: SmartwatchViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    AnemomScreen(
        title = "Smartwatch & PPG",
        subtitle = "Rekam sinyal dari gelang/band",
        onBack = onBack
    ) {
        AnemomCard(Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Watch, null, tint = Pink500, modifier = Modifier.size(28.dp))
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text("Anemom Band", style = MaterialTheme.typography.titleMedium, color = Maroon)
                    Text("Tersambung (simulasi)", style = MaterialTheme.typography.bodySmall, color = MutedText)
                }
                AnemomChip("Data simulasi", AmberBg, AmberText)
            }
        }

        Spacer(Modifier.height(14.dp))
        AnemomCard(Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.MonitorHeart, null, tint = Pink500, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(8.dp))
                Text("Rekam PPG 60 detik", style = MaterialTheme.typography.titleMedium, color = Maroon)
            }
            Spacer(Modifier.height(10.dp))
            when (state.stage) {
                WatchStage.IDLE -> Text(
                    "Duduk tenang, letakkan tangan di atas meja, lalu tekan Mulai rekam.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MutedText
                )
                WatchStage.RECORDING -> {
                    val progress = (RECORD_SECONDS - state.secondsLeft) / RECORD_SECONDS.toFloat()
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth().height(8.dp),
                        color = Pink500,
                        trackColor = Pink100,
                        strokeCap = StrokeCap.Round
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Merekam... sisa ${state.secondsLeft} detik",
                        style = MaterialTheme.typography.bodySmall,
                        color = MutedText
                    )
                }
                WatchStage.DONE -> {
                    Row(Modifier.fillMaxWidth()) {
                        Metric("Detak jantung", "${state.heartRate}", "bpm", Modifier.weight(1f))
                        Metric("SpO2", "${state.spo2}", "%", Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(8.dp))
                    AnemomChip("Kualitas sinyal: ${state.signalQuality}", GreenBg, GreenText)
                }
            }
        }

        Spacer(Modifier.height(14.dp))
        when (state.stage) {
            WatchStage.IDLE -> GradientButton("Mulai rekam", onClick = viewModel::startRecording)
            WatchStage.RECORDING -> GradientButton("Sedang merekam...", onClick = {}, enabled = false)
            WatchStage.DONE -> {
                GradientButton("Rekam ulang", onClick = viewModel::startRecording)
                Spacer(Modifier.height(10.dp))
                AnemomOutlinedButton("Input Hb darah", onClick = onInputHb)
            }
        }
        Spacer(Modifier.height(12.dp))
        NoteCard("Versi ini memakai data simulasi. Koneksi band (BLE) dan estimasi Hb dari PPG belum tersedia. PPG hanya skrining awal, bukan tes darah.")
    }
}

@Composable
private fun Metric(label: String, value: String, unit: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MutedText)
        Row(verticalAlignment = Alignment.Bottom) {
            Text(value, fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, color = Maroon)
            Text(" $unit", style = MaterialTheme.typography.bodySmall, color = MutedText)
        }
    }
}

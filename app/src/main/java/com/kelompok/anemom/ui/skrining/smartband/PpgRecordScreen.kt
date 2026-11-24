package com.kelompok.anemom.ui.skrining.smartband

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.MonitorHeart
import androidx.compose.material.icons.outlined.Watch
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kelompok.anemom.data.smartband.PpgQuality
import com.kelompok.anemom.ui.AppViewModelProvider
import com.kelompok.anemom.ui.components.AnemomCard
import com.kelompok.anemom.ui.components.AnemomChip
import com.kelompok.anemom.ui.components.AnemomOutlinedButton
import com.kelompok.anemom.ui.components.AnemomScreen
import com.kelompok.anemom.ui.components.GradientButton
import com.kelompok.anemom.ui.components.NoteCard
import com.kelompok.anemom.ui.theme.AmberBg
import com.kelompok.anemom.ui.theme.AmberText
import com.kelompok.anemom.ui.theme.ErrorRed
import com.kelompok.anemom.ui.theme.GreenBg
import com.kelompok.anemom.ui.theme.GreenText
import com.kelompok.anemom.ui.theme.Maroon
import com.kelompok.anemom.ui.theme.MutedText
import com.kelompok.anemom.ui.theme.Pink100
import com.kelompok.anemom.ui.theme.Pink500
import com.kelompok.anemom.ui.theme.RedBg
import kotlin.math.roundToInt

/**
 * Smartband & PPG: dibuka dari tombol "Rekam sinyal PPG" di Koneksi Bluetooth.
 * Hitung mundur 10 detik, lalu gelombang PPG tampil selama rekaman, lalu kirim ke server.
 */
@Composable
fun PpgRecordScreen(
    onBack: () -> Unit,
    onManageConnection: () -> Unit,
    viewModel: PpgRecordViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val device = state.device

    AnemomScreen(
        title = "Smartband & PPG",
        subtitle = if (device != null) "Terhubung untuk pemantauan awal" else "Smartband belum tersambung",
        onBack = onBack
    ) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size(112.dp)
                    .clip(CircleShape)
                    .background(Pink100),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.Watch, contentDescription = null, tint = Maroon, modifier = Modifier.size(60.dp))
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
        ) {
            if (device != null) {
                AnemomChip("Bluetooth terhubung · Sensor aktif", containerColor = GreenBg, contentColor = GreenText)
            } else {
                AnemomChip("Belum tersambung", containerColor = RedBg, contentColor = ErrorRed)
            }
            if (state.isSimulated) {
                AnemomChip("Data simulasi", containerColor = Pink100, contentColor = Pink500)
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = device?.name ?: "Smartband",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = Maroon,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        device?.detailLine()?.takeIf { it.isNotEmpty() }?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = MutedText,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(Modifier.height(16.dp))
        PpgCard(state)

        Spacer(Modifier.height(14.dp))
        NoteCard(
            "PPG merekam perubahan aliran darah di bawah kulit. Sinyal dianalisis di server; " +
                "hasilnya estimasi skrining awal, bukan kadar Hb darah. " +
                "Tetap diam dan pakai smartband dengan pas saat merekam."
        )

        Spacer(Modifier.height(16.dp))
        when (state.stage) {
            PpgStage.PREPARING, PpgStage.RECORDING -> AnemomOutlinedButton(
                text = "Batalkan rekaman",
                onClick = onBack,
                modifier = Modifier.fillMaxWidth()
            )

            PpgStage.DONE -> {
                state.errorMessage?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = ErrorRed)
                    Spacer(Modifier.height(8.dp))
                }
                GradientButton(
                    text = "Kirim untuk dianalisis",
                    onClick = viewModel::upload,
                    modifier = Modifier.fillMaxWidth(),
                    icon = Icons.Outlined.CloudUpload
                )
                Spacer(Modifier.height(10.dp))
                AnemomOutlinedButton(
                    text = "Rekam ulang PPG",
                    onClick = viewModel::start,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            PpgStage.UPLOADING -> GradientButton(
                text = "Mengirim...",
                onClick = {},
                modifier = Modifier.fillMaxWidth(),
                enabled = false,
                isLoading = true
            )

            PpgStage.UPLOADED -> {
                // TODO(backend): saat analisis server siap, arahkan ke layar Hasil skrining
                //  memakai state.recordingId.
                AnemomCard(Modifier.fillMaxWidth(), containerColor = GreenBg) {
                    Text(
                        text = if (state.isSimulated) {
                            "Rekaman terkirim (simulasi). Analisis PPG di server belum tersedia, jadi belum ada hasil."
                        } else {
                            "Rekaman terkirim. Hasil analisis akan muncul setelah diproses server."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = GreenText
                    )
                }
                Spacer(Modifier.height(12.dp))
                GradientButton(
                    text = "Rekam ulang PPG",
                    onClick = viewModel::start,
                    modifier = Modifier.fillMaxWidth(),
                    icon = Icons.Outlined.MonitorHeart
                )
            }

            PpgStage.ERROR -> GradientButton(
                text = "Coba lagi",
                onClick = viewModel::start,
                modifier = Modifier.fillMaxWidth(),
                icon = Icons.Outlined.MonitorHeart
            )
        }

        Spacer(Modifier.height(4.dp))
        TextButton(onClick = onManageConnection, modifier = Modifier.fillMaxWidth()) {
            Text("Kelola koneksi Bluetooth", color = Pink500, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun PpgCard(state: PpgRecordUiState) {
    AnemomCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Sinyal PPG",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Maroon,
                modifier = Modifier.weight(1f)
            )
            StageChip(state.stage)
        }

        Spacer(Modifier.height(12.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            contentAlignment = Alignment.Center
        ) {
            when (state.stage) {
                PpgStage.PREPARING -> Countdown(state.countdown)
                PpgStage.ERROR -> Text(
                    text = state.errorMessage ?: "Rekaman terhenti.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ErrorRed,
                    textAlign = TextAlign.Center
                )
                else -> PpgWaveform(
                    samples = state.wave,
                    capacity = state.waveCapacity,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        if (state.stage != PpgStage.PREPARING && state.stage != PpgStage.ERROR) {
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth()) {
                Stat(
                    value = state.quality?.label ?: "-",
                    label = "Kualitas sinyal",
                    valueColor = qualityColor(state.quality),
                    alignEnd = false,
                    modifier = Modifier.weight(1f)
                )
                Stat(
                    value = "${state.recordedSeconds} detik",
                    label = "Durasi rekaman",
                    valueColor = Maroon,
                    alignEnd = true,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = when (state.stage) {
                    PpgStage.PREPARING -> "Bersiap merekam"
                    PpgStage.RECORDING -> "Merekam..."
                    PpgStage.ERROR -> "Rekaman terhenti"
                    else -> "Rekaman selesai"
                },
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = Maroon,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "${state.recordedSeconds}/$RECORD_SECONDS detik · ${(state.progress * 100).roundToInt()}%",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = Pink500
            )
        }
        Spacer(Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { state.progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp),
            color = Pink500,
            trackColor = Pink100,
            strokeCap = StrokeCap.Round
        )
    }
}

@Composable
private fun StageChip(stage: PpgStage) {
    when (stage) {
        PpgStage.PREPARING -> AnemomChip("Bersiap", containerColor = AmberBg, contentColor = AmberText)
        PpgStage.RECORDING -> AnemomChip("Merekam", containerColor = Pink100, contentColor = Pink500)
        PpgStage.DONE, PpgStage.UPLOADING -> AnemomChip("Selesai", containerColor = GreenBg, contentColor = GreenText)
        PpgStage.UPLOADED -> AnemomChip("Terkirim", containerColor = GreenBg, contentColor = GreenText)
        PpgStage.ERROR -> AnemomChip("Terhenti", containerColor = RedBg, contentColor = ErrorRed)
    }
}

@Composable
private fun Countdown(seconds: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("$seconds", fontSize = 44.sp, fontWeight = FontWeight.ExtraBold, color = Pink500)
        Text(
            text = "Mulai merekam dalam $seconds detik",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = Maroon
        )
        Text(
            text = "Duduk tenang dan letakkan tangan di atas meja.",
            style = MaterialTheme.typography.bodySmall,
            color = MutedText,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun Stat(
    value: String,
    label: String,
    valueColor: androidx.compose.ui.graphics.Color,
    alignEnd: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = if (alignEnd) Alignment.End else Alignment.Start
    ) {
        Text(value, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = valueColor)
        Text(label, style = MaterialTheme.typography.bodySmall, color = MutedText)
    }
}

private fun qualityColor(quality: PpgQuality?) = when (quality) {
    PpgQuality.BAIK -> GreenText
    PpgQuality.SEDANG -> AmberText
    PpgQuality.LEMAH -> ErrorRed
    null -> MutedText
}

/**
 * Gelombang PPG kanal merah. Mengisi dari kiri sampai [capacity] sampel, lalu bergeser.
 * Skala vertikal mengikuti nilai terkecil/terbesar di jendela yang tampil.
 */
@Composable
private fun PpgWaveform(samples: List<Float>, capacity: Int, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        if (samples.size < 2) return@Canvas
        val min = samples.min()
        val max = samples.max()
        val range = (max - min).takeIf { it > 1e-6f } ?: 1f
        val padY = size.height * 0.08f
        val usableHeight = size.height - 2 * padY
        val slots = maxOf(capacity, samples.size) - 1
        val step = size.width / slots

        val path = Path()
        samples.forEachIndexed { i, v ->
            val x = i * step
            val y = padY + (1f - (v - min) / range) * usableHeight
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(
            path = path,
            color = Pink500,
            style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}

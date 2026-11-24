package com.kelompok.anemom.ui.riwayat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kelompok.anemom.data.model.HbPoint
import com.kelompok.anemom.data.model.HbResult
import com.kelompok.anemom.data.model.HbRisk
import com.kelompok.anemom.data.repository.HbRepository
import com.kelompok.anemom.ui.AppViewModelProvider
import com.kelompok.anemom.ui.components.AnemomCard
import com.kelompok.anemom.ui.components.AnemomChip
import com.kelompok.anemom.ui.components.AnemomScreen
import com.kelompok.anemom.ui.components.GradientButton
import com.kelompok.anemom.ui.home.HbTrendChart
import com.kelompok.anemom.ui.theme.AmberText
import com.kelompok.anemom.ui.theme.ErrorRed
import com.kelompok.anemom.ui.theme.GreenText
import com.kelompok.anemom.ui.theme.Maroon
import com.kelompok.anemom.ui.theme.MutedText
import com.kelompok.anemom.ui.theme.Pink100
import com.kelompok.anemom.ui.theme.Pink500
import com.kelompok.anemom.util.DateFormats
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import kotlin.math.abs

data class HbChartUiState(
    val isLoaded: Boolean = false,
    /** Hasil dalam rentang yang ditampilkan, terlama dulu. */
    val shown: List<HbResult> = emptyList(),
    val rangeLabel: String = "3 bulan terakhir",
    val latest: HbResult? = null,
    val previous: HbResult? = null
)

class HbChartViewModel(repository: HbRepository) : ViewModel() {
    val uiState: StateFlow<HbChartUiState> = repository.results.map { all ->
        val sorted = all.sortedBy { it.date }
        val from = LocalDate.now().minusMonths(3)
        val recent = sorted.filter { !it.date.isBefore(from) }
        // Kurang dari 2 titik dalam 3 bulan: tampilkan 8 hasil terakhir supaya tetap ada tren.
        val (shown, label) = if (recent.size >= 2) recent to "3 bulan terakhir" else sorted.takeLast(8) to "Hasil terakhir"
        HbChartUiState(
            isLoaded = true,
            shown = shown,
            rangeLabel = label,
            latest = sorted.lastOrNull(),
            previous = sorted.getOrNull(sorted.size - 2)
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HbChartUiState())
}

/** Grafik Hb darah: dibuka dari kartu Tren Hb di Beranda ("Lihat detail"). Hanya hasil laboratorium. */
@Composable
fun HbChartScreen(
    onBack: () -> Unit,
    onAddHb: () -> Unit,
    viewModel: HbChartViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    AnemomScreen(title = "Grafik Hb darah", subtitle = "Tren hasil pemeriksaan laboratorium", onBack = onBack) {
        if (!state.isLoaded) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 48.dp),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator(color = Pink500) }
        } else {
            val first = state.shown.firstOrNull()
            val last = state.shown.lastOrNull()
            Row(verticalAlignment = Alignment.CenterVertically) {
                AnemomChip(state.rangeLabel, containerColor = Pink100, contentColor = Pink500)
                if (first != null && last != null) {
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "${DateFormats.dayMonth(first.date)} – ${DateFormats.short(last.date)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MutedText
                    )
                }
            }

            Spacer(Modifier.height(14.dp))
            val latest = state.latest
            AnemomCard(Modifier.fillMaxWidth()) {
                if (latest == null) {
                    Text("Belum ada hasil Hb darah.", style = MaterialTheme.typography.bodyMedium, color = MutedText)
                } else {
                    Text("Hb darah terbaru · ${DateFormats.long(latest.date)}", style = MaterialTheme.typography.bodySmall, color = MutedText)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "${hbText(latest.value)} g/dL",
                            fontSize = 34.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Maroon,
                            modifier = Modifier.weight(1f)
                        )
                        HistoryItem.Hb(latest).chip()?.let { AnemomChip(it.text, containerColor = it.container, contentColor = it.content) }
                    }
                    state.previous?.let { prev ->
                        val diff = latest.value - prev.value
                        val text = when {
                            abs(diff) < 0.05f -> "Sama dengan pemeriksaan ${DateFormats.dayMonth(prev.date)}"
                            diff > 0 -> "↑ ${hbText(diff)} g/dL dari pemeriksaan ${DateFormats.dayMonth(prev.date)}"
                            else -> "↓ ${hbText(-diff)} g/dL dari pemeriksaan ${DateFormats.dayMonth(prev.date)}"
                        }
                        Text(text, style = MaterialTheme.typography.bodySmall, color = if (diff >= 0) GreenText else ErrorRed)
                    }
                }
            }

            Spacer(Modifier.height(14.dp))
            AnemomCard(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Hemoglobin (g/dL)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Maroon,
                        modifier = Modifier.weight(1f)
                    )
                    Box(
                        Modifier
                            .size(10.dp)
                            .clip(RoundedCornerShape(50))
                            .background(Pink500)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("Lab", style = MaterialTheme.typography.labelMedium, color = Maroon)
                }
                Spacer(Modifier.height(10.dp))
                if (state.shown.size >= 2) {
                    HbTrendChart(
                        points = state.shown.map { HbPoint(DateFormats.dayMonth(it.date), it.value) },
                        height = 200.dp
                    )
                } else {
                    Text(
                        "Grafik muncul setelah ada minimal 2 hasil Hb darah.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MutedText
                    )
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    "Skor AI dan sinyal PPG tidak ditampilkan karena bukan kadar Hb.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MutedText
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "– – Rujukan aplikasi: ${hbText(HbRisk.REFERENCE_HB)} g/dL\nHb <${hbText(HbRisk.REFERENCE_HB)} g/dL perlu evaluasi tenaga kesehatan.",
                    style = MaterialTheme.typography.labelSmall,
                    color = AmberText
                )
            }

            Spacer(Modifier.height(14.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Pink100.copy(alpha = 0.75f))
                    .padding(14.dp)
            ) {
                Icon(Icons.Outlined.Info, contentDescription = null, tint = Pink500, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Text(
                    "Grafik hanya memuat hasil tes darah. Skor AI dan sinyal PPG tidak dimasukkan sebagai kadar Hb.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Maroon
                )
            }

            Spacer(Modifier.height(18.dp))
            GradientButton(text = "Tambah hasil Hb darah", onClick = onAddHb, modifier = Modifier.fillMaxWidth(), icon = Icons.Filled.Add)
        }
    }
}

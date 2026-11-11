package com.kelompok.anemom.ui.riwayat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kelompok.anemom.data.model.HbRisk
import com.kelompok.anemom.ui.AppViewModelProvider
import com.kelompok.anemom.ui.components.AnemomCard
import com.kelompok.anemom.ui.components.AnemomChip
import com.kelompok.anemom.ui.components.AnemomScreen
import com.kelompok.anemom.ui.components.GradientButton
import com.kelompok.anemom.ui.components.NoteCard
import com.kelompok.anemom.ui.components.RiskChip
import com.kelompok.anemom.ui.home.HbTrendChart
import com.kelompok.anemom.ui.theme.Maroon
import com.kelompok.anemom.ui.theme.MutedText
import com.kelompok.anemom.ui.theme.Pink100
import com.kelompok.anemom.ui.theme.Pink500
import com.kelompok.anemom.util.DateFormats

/** Tab Riwayat: grafik dan daftar hasil Hb darah. */
@Composable
fun RiwayatScreen(
    onBack: () -> Unit,
    onAddHb: () -> Unit,
    viewModel: RiwayatViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    AnemomScreen(title = "Grafik Hb darah", subtitle = "Pantau tren Hb Bunda", onBack = onBack) {
        AnemomCard(Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Tren Hb",
                    style = MaterialTheme.typography.titleMedium,
                    color = Maroon,
                    modifier = Modifier.weight(1f)
                )
                AnemomChip("Hasil laboratorium", Pink100, Pink500)
            }
            Spacer(Modifier.height(8.dp))
            if (state.chartPoints.isEmpty()) {
                Text(
                    text = "Belum ada hasil Hb. Tambahkan hasil tes darah Bunda untuk melihat grafik.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MutedText,
                    modifier = Modifier.padding(vertical = 24.dp)
                )
            } else {
                HbTrendChart(points = state.chartPoints, height = 200.dp)
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Garis putus-putus: batas anemia ${HbRisk.REFERENCE_HB.toInt()} g/dL (rujukan aplikasi)",
                style = MaterialTheme.typography.bodySmall,
                color = MutedText
            )
        }

        state.latest?.let { latest ->
            Spacer(Modifier.height(14.dp))
            AnemomCard(Modifier.fillMaxWidth()) {
                Text("Hasil terakhir", style = MaterialTheme.typography.titleMedium, color = Maroon)
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${DateFormats.hb(latest.value)} g/dL",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Maroon,
                        modifier = Modifier.weight(1f)
                    )
                    state.latestRisk?.let { RiskChip(it) }
                }
                Text(
                    text = DateFormats.long(latest.date),
                    style = MaterialTheme.typography.bodySmall,
                    color = MutedText
                )
            }
        }

        Spacer(Modifier.height(18.dp))
        Text("Riwayat hasil", style = MaterialTheme.typography.titleMedium, color = Maroon)
        Spacer(Modifier.height(8.dp))

        if (state.results.isEmpty() && state.isLoaded) {
            Text(
                text = "Belum ada riwayat.",
                style = MaterialTheme.typography.bodySmall,
                color = MutedText
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            state.results.forEach { result ->
                AnemomCard(Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = "${DateFormats.hb(result.value)} g/dL",
                                style = MaterialTheme.typography.titleMedium,
                                color = Maroon
                            )
                            Text(
                                text = buildString {
                                    append(DateFormats.short(result.date))
                                    if (result.facility.isNotBlank()) append(" · ${result.facility}")
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MutedText
                            )
                        }
                        RiskChip(HbRisk.classify(result.value))
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        GradientButton(text = "Tambah hasil Hb", onClick = onAddHb, icon = Icons.Filled.Add)
        Spacer(Modifier.height(12.dp))
        NoteCard("Grafik hanya menampilkan hasil tes darah yang Bunda masukkan. Konsultasikan hasil dengan bidan atau dokter.")
    }
}

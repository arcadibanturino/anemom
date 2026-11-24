package com.kelompok.anemom.ui.riwayat

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kelompok.anemom.ui.AppViewModelProvider
import com.kelompok.anemom.ui.components.AnemomChip
import com.kelompok.anemom.ui.components.AnemomOutlinedButton
import com.kelompok.anemom.ui.components.AnemomTabHeader
import com.kelompok.anemom.ui.theme.CardBorder
import com.kelompok.anemom.ui.theme.Maroon
import com.kelompok.anemom.ui.theme.MutedText
import com.kelompok.anemom.ui.theme.Pink100
import com.kelompok.anemom.ui.theme.Pink500
import java.time.format.DateTimeFormatter

private val monthFormat = DateTimeFormatter.ofPattern("MMMM yyyy", indonesian)
private val dateTimeFormat = DateTimeFormatter.ofPattern("d MMM yyyy · HH.mm", indonesian)
private val dateFormat = DateTimeFormatter.ofPattern("d MMM yyyy", indonesian)

/** Tab Riwayat: Riwayat pemeriksaan (Hb lab, AI Scan Mata, PPG). Grafik Hb pindah ke Beranda. */
@Suppress("UNUSED_PARAMETER")
@Composable
fun RiwayatScreen(
    onBack: () -> Unit,
    onAddHb: () -> Unit,
    onOpenDetail: (HistoryKind, Long) -> Unit = { _, _ -> },
    viewModel: RiwayatViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        AnemomTabHeader(title = "Riwayat pemeriksaan", subtitle = "Catatan kesehatan Bunda")
        Spacer(Modifier.height(16.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            HistoryFilter.entries.forEach { f ->
                val selected = f == state.selectedFilter
                Text(
                    text = f.label,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = if (selected) Color.White else Pink500,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(if (selected) Pink500 else Pink100)
                        .clickable { viewModel.setFilter(f) }
                        .padding(horizontal = 14.dp, vertical = 7.dp)
                )
            }
        }

        when {
            !state.isLoaded -> Box(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 48.dp),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator(color = Pink500) }

            state.months.isEmpty() -> {
                Spacer(Modifier.height(24.dp))
                Text(
                    text = "Belum ada pemeriksaan. Catat hasil Hb darah atau lakukan skrining dari tab Skrining.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MutedText
                )
                Spacer(Modifier.height(12.dp))
                AnemomOutlinedButton("Tambah hasil Hb darah", onClick = onAddHb, modifier = Modifier.fillMaxWidth())
            }

            else -> state.months.forEach { month ->
                Spacer(Modifier.height(20.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = month.month.format(monthFormat).replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp),
                        fontWeight = FontWeight.Bold,
                        color = Maroon,
                        modifier = Modifier.weight(1f)
                    )
                    Text("${month.items.size} pemeriksaan", style = MaterialTheme.typography.labelMedium, color = Pink500)
                }
                Spacer(Modifier.height(10.dp))
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    month.items.forEach { item -> HistoryCard(item) { onOpenDetail(item.kind, item.id) } }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun HistoryCard(item: HistoryItem, onClick: () -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Color.White)
            .border(1.dp, CardBorder, shape)
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(item.icon(), contentDescription = null, tint = Pink500, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(item.title(), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Maroon)
                Text(
                    text = if (item is HistoryItem.Hb) item.at.format(dateFormat) else item.at.format(dateTimeFormat),
                    style = MaterialTheme.typography.labelSmall,
                    color = MutedText
                )
            }
            Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = Pink500)
        }
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = item.mainValue(),
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Maroon,
                modifier = Modifier.weight(1f)
            )
            item.chip()?.let { AnemomChip(it.text, containerColor = it.container, contentColor = it.content) }
        }
        Spacer(Modifier.height(8.dp))
        Text(item.sourceLine(), style = MaterialTheme.typography.labelSmall, color = MutedText)
    }
}

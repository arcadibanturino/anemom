package com.kelompok.anemom.ui.skrining

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.Watch
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.kelompok.anemom.ui.components.AnemomScreen
import com.kelompok.anemom.ui.components.NoteCard
import com.kelompok.anemom.ui.theme.CardBorder
import com.kelompok.anemom.ui.theme.Maroon
import com.kelompok.anemom.ui.theme.MutedText
import com.kelompok.anemom.ui.theme.Pink100
import com.kelompok.anemom.ui.theme.Pink500

/** Tab Skrining (tengah): pintu masuk ke AI Scan, Smartwatch, dan Input Hb. */
@Composable
fun SkriningScreen(
    onBack: () -> Unit,
    onAiScan: () -> Unit,
    onSmartwatch: () -> Unit,
    onInputHb: () -> Unit
) {
    AnemomScreen(
        title = "Skrining anemia",
        subtitle = "Pilih cara skrining untuk Bunda",
        onBack = onBack
    ) {
        SkriningOption(
            icon = Icons.Outlined.Visibility,
            title = "AI Scan Mata",
            description = "Foto kelopak mata bagian dalam untuk estimasi awal.",
            onClick = onAiScan
        )
        Spacer(Modifier.height(12.dp))
        SkriningOption(
            icon = Icons.Outlined.Watch,
            title = "Smartwatch & PPG",
            description = "Rekam sinyal PPG dari gelang/band selama 60 detik.",
            onClick = onSmartwatch
        )
        Spacer(Modifier.height(12.dp))
        SkriningOption(
            icon = Icons.Outlined.EditNote,
            title = "Input Hb darah",
            description = "Catat hasil tes darah dari laboratorium atau puskesmas.",
            onClick = onInputHb
        )
        Spacer(Modifier.height(16.dp))
        NoteCard("AI dan PPG hanya skrining awal, bukan tes darah. Konfirmasi dengan pemeriksaan Hb darah.")
    }
}

@Composable
private fun SkriningOption(
    icon: ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White)
            .border(1.dp, CardBorder, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Pink100),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = Pink500, modifier = Modifier.size(28.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = Maroon)
            Text(description, style = MaterialTheme.typography.bodySmall, color = MutedText)
        }
        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = MutedText)
    }
}

package com.kelompok.anemom.ui.skrining

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.Icon
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kelompok.anemom.ui.components.AnemomCard
import com.kelompok.anemom.ui.components.AnemomChip
import com.kelompok.anemom.ui.components.AnemomOutlinedButton
import com.kelompok.anemom.ui.components.AnemomScreen
import com.kelompok.anemom.ui.components.GradientButton
import com.kelompok.anemom.ui.components.NoteCard
import com.kelompok.anemom.ui.theme.AmberBg
import com.kelompok.anemom.ui.theme.AmberText
import com.kelompok.anemom.ui.theme.Maroon
import com.kelompok.anemom.ui.theme.MutedText
import com.kelompok.anemom.ui.theme.Pink100
import com.kelompok.anemom.ui.theme.Pink500

/**
 * AI Scan Mata (tampilan saja).
 * TODO(fitur): CameraX preview + model YOLOv8 TFLite (segmentasi konjungtiva -> estimasi Hb).
 */
@Composable
fun AiScanScreen(onBack: () -> Unit, onInputHb: () -> Unit) {
    var message by remember { mutableStateOf<String?>(null) }

    AnemomScreen(
        title = "AI Scan Mata",
        subtitle = "Estimasi awal dari konjungtiva",
        onBack = onBack
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(24.dp))
                .background(Pink100)
                .border(2.dp, Pink500.copy(alpha = 0.5f), RoundedCornerShape(24.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Outlined.Visibility,
                contentDescription = null,
                tint = Pink500,
                modifier = Modifier.size(72.dp)
            )
            AnemomChip(
                text = "Pratinjau kamera",
                containerColor = AmberBg,
                contentColor = AmberText,
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 12.dp)
            )
        }
        Spacer(Modifier.height(14.dp))
        AnemomCard(Modifier.fillMaxWidth()) {
            Text("Cara memotret", style = MaterialTheme.typography.titleMedium, color = Maroon)
            Spacer(Modifier.height(6.dp))
            listOf(
                "Cari ruangan dengan cahaya yang cukup.",
                "Tarik kelopak mata bawah perlahan sampai bagian dalam yang merah muda terlihat.",
                "Posisikan mata di dalam bingkai, lalu tahan stabil."
            ).forEachIndexed { i, step ->
                Text(
                    text = "${i + 1}. $step",
                    style = MaterialTheme.typography.bodySmall,
                    color = MutedText
                )
            }
        }
        Spacer(Modifier.height(14.dp))
        GradientButton(
            text = "Ambil foto",
            icon = Icons.Outlined.CameraAlt,
            onClick = { message = "Kamera dan model AI belum tersedia di versi ini." }
        )
        message?.let {
            Spacer(Modifier.height(8.dp))
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = AmberText,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
        Spacer(Modifier.height(10.dp))
        AnemomOutlinedButton(text = "Input Hb darah", onClick = onInputHb)
        Spacer(Modifier.height(12.dp))
        NoteCard("Hasil AI hanya estimasi dan bukan diagnosis. Konfirmasi dengan pemeriksaan Hb darah.")
    }
}

package com.kelompok.anemom.ui.skrining

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.Watch
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kelompok.anemom.ui.components.AnemomChip
import com.kelompok.anemom.ui.components.NoteCard
import com.kelompok.anemom.ui.theme.CardBorder
import com.kelompok.anemom.ui.theme.Maroon
import com.kelompok.anemom.ui.theme.MutedText
import com.kelompok.anemom.ui.theme.Pink100
import com.kelompok.anemom.ui.theme.Pink500

/**
 * Tab Skrining (tengah): pintu masuk ke AI Scan, Smartband/PPG, dan Input Hb.
 *
 * Ini layar utama sebuah tab, jadi tidak ada tombol kembali dan tidak ada ikon hati
 * (ikon hati hanya di Beranda). Karena itu header-nya tidak memakai AnemomScreen/AnemomTopBar.
 * Parameter onBack dipertahankan agar pemanggil di MainScreen tidak perlu diubah.
 */
@Suppress("UNUSED_PARAMETER")
@Composable
fun SkriningScreen(
    onBack: () -> Unit,
    onAiScan: () -> Unit,
    onSmartband: () -> Unit,
    onInputHb: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        TabHeader(
            title = "Skrining anemia",
            subtitle = "Kenali lebih awal, rawat lebih baik"
        )

        Spacer(Modifier.height(24.dp))
        Text(
            text = "Pilih pemeriksaan\nuntuk Bunda",
            style = MaterialTheme.typography.headlineSmall.copy(
                fontSize = 24.sp,
                lineHeight = 31.sp
            ),
            fontWeight = FontWeight.Bold,
            color = Maroon
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Mulai skrining awal atau catat hasil Hb darah dari laboratorium.",
            style = MaterialTheme.typography.bodyMedium,
            color = MutedText
        )

        Spacer(Modifier.height(20.dp))
        SkriningOption(
            icon = Icons.Outlined.Visibility,
            title = "AI Scan Mata",
            description = "Skrining melalui foto konjungtiva mata.",
            tag = "Skrining awal · ±1 menit",
            onClick = onAiScan
        )
        Spacer(Modifier.height(12.dp))
        SkriningOption(
            icon = Icons.Outlined.Watch,
            title = "Smartband / PPG",
            description = "Rekam sinyal optik dari smartband Bunda.",
            tag = "Skrining awal · Perangkat diperlukan",
            onClick = onSmartband
        )
        Spacer(Modifier.height(12.dp))
        SkriningOption(
            icon = Icons.Outlined.WaterDrop,
            title = "Input Hb darah",
            description = "Masukkan nilai, tanggal, dan sumber hasil tes.",
            tag = "Hasil tes darah terukur",
            onClick = onInputHb
        )

        Spacer(Modifier.height(16.dp))
        NoteCard("Skor AI dan sinyal PPG bukan kadar Hb darah dan tidak dapat menegakkan diagnosis anemia.")
        Spacer(Modifier.height(24.dp))
    }
}

/**
 * Header untuk layar utama tab: rata kiri sejajar konten, tanpa tombol kembali dan tanpa ikon.
 * Judul 20sp, sedikit lebih kecil dari judul konten (24sp), supaya keduanya tidak bersaing.
 */
@Composable
private fun TabHeader(title: String, subtitle: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge.copy(
                fontSize = 20.sp,
                lineHeight = 26.sp
            ),
            fontWeight = FontWeight.SemiBold,
            color = Maroon
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
            color = MutedText
        )
    }
}

@Composable
private fun SkriningOption(
    icon: ImageVector,
    title: String,
    description: String,
    tag: String,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White)
            .border(1.dp, CardBorder, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Pink100),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = Pink500, modifier = Modifier.size(26.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Maroon
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MutedText
                )
            }
            Spacer(Modifier.width(8.dp))
            Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = Maroon)
        }
        Spacer(Modifier.height(12.dp))
        AnemomChip(text = tag, containerColor = Pink100, contentColor = Pink500)
    }
}

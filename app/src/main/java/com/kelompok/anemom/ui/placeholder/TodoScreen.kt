package com.kelompok.anemom.ui.placeholder

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Construction
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kelompok.anemom.ui.components.AnemomChip
import com.kelompok.anemom.ui.components.AnemomTopBar
import com.kelompok.anemom.ui.theme.Maroon
import com.kelompok.anemom.ui.theme.MutedText
import com.kelompok.anemom.ui.theme.Pink100
import com.kelompok.anemom.ui.theme.Pink500

/** Isi halaman sementara untuk fitur yang belum dikerjakan (TODO). */
@Composable
fun FeaturePlaceholder(
    title: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .background(Pink100, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Construction,
                contentDescription = null,
                tint = Pink500,
                modifier = Modifier.size(44.dp)
            )
        }
        Spacer(Modifier.height(20.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = Maroon,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(10.dp))
        AnemomChip(text = "SEGERA HADIR", containerColor = Pink100, contentColor = Pink500)
        Spacer(Modifier.height(14.dp))
        Text(
            text = todoDescription(title),
            style = MaterialTheme.typography.bodyMedium,
            color = MutedText,
            textAlign = TextAlign.Center
        )
    }
}

/** Halaman penuh (dengan tombol kembali) untuk fitur yang dibuka dari Beranda. */
@Composable
fun TodoScreen(
    title: String,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp)
    ) {
        AnemomTopBar(title = title, onBack = onBack)
        FeaturePlaceholder(title = title, modifier = Modifier.weight(1f))
    }
}

// TODO(fitur): ganti dengan implementasi sungguhan tiap fitur.
private fun todoDescription(title: String): String = when (title) {
    "AI Scan Mata", "Skrining" ->
        "TODO: ambil foto area pinggiran mata dengan kamera, lalu jalankan model AI untuk memprediksi gejala anemia."
    "Smartwatch" ->
        "TODO: sambungkan band/smartwatch dan baca sinyal PPG untuk skrining awal."
    "Grafik Hb" ->
        "TODO: grafik riwayat Hb lengkap dengan filter periode."
    "Nutrisi" ->
        "TODO: rekomendasi makanan kaya zat besi untuk ibu hamil."
    "Scan Makanan" ->
        "TODO: scan makanan untuk melihat kandungan zat besinya."
    "Panduan aman" ->
        "TODO: panduan keamanan dan tanda bahaya selama kehamilan."
    "TTD" ->
        "TODO: pengingat dan pencatatan konsumsi Tablet Tambah Darah."
    "Riwayat" ->
        "TODO: riwayat hasil skrining dan pemeriksaan Hb."
    else ->
        "Fitur ini sedang dikembangkan."
}

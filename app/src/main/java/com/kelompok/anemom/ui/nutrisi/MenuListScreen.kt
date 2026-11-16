package com.kelompok.anemom.ui.nutrisi

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kelompok.anemom.ui.components.AnemomCard
import com.kelompok.anemom.ui.components.BackCircleButton
import com.kelompok.anemom.ui.theme.AmberText
import com.kelompok.anemom.ui.theme.CardBorder
import com.kelompok.anemom.ui.theme.Maroon
import com.kelompok.anemom.ui.theme.MutedText
import com.kelompok.anemom.ui.theme.Pink100
import com.kelompok.anemom.ui.theme.Pink500

/** Daftar lengkap rekomendasi menu makanan (dibuka dari "Lihat Semua" di Beranda). */
@Composable
fun MenuListScreen(
    onBack: () -> Unit,
    onOpenDetail: (menuTitle: String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BackCircleButton(onClick = onBack)
            Spacer(Modifier.width(12.dp))
            Text(
                text = "Rekomendasi Makanan",
                style = MaterialTheme.typography.titleLarge,
                color = Maroon
            )
        }

        Spacer(Modifier.height(8.dp))
        Text(
            text = "Menu Bergizi Bunda",
            style = MaterialTheme.typography.headlineMedium,
            color = Maroon
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Pilihan makanan bergizi untuk membantu memenuhi kebutuhan nutrisi Bunda.",
            style = MaterialTheme.typography.bodyMedium,
            color = MutedText
        )
        Spacer(Modifier.height(16.dp))

        AnemomCard(modifier = Modifier.fillMaxWidth(), containerColor = Pink100) {
            Text(
                text = "Variasikan isi piring Bunda",
                style = MaterialTheme.typography.titleMedium,
                color = Maroon
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Padukan sumber zat besi, protein, sayur, dan buah. " +
                        "Nilai zat besi berikut adalah perkiraan per porsi.",
                style = MaterialTheme.typography.bodySmall,
                color = Maroon
            )
        }
        Spacer(Modifier.height(16.dp))

        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            menuRecommendations.forEach { menu ->
                MenuListItem(menu = menu, onClick = { onOpenDetail(menu.title) })
            }
        }

        Spacer(Modifier.height(20.dp))
        Text(
            text = "Rujukan gizi: NIH ODS, Iron fact sheet.\n" +
                    "Keamanan kehamilan: NHS, Foods to avoid.\n" +
                    "Tetap konsumsi TTD sesuai anjuran bidan atau dokter.",
            style = MaterialTheme.typography.bodySmall,
            color = MutedText
        )
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun MenuListItem(menu: MenuRecommendation, onClick: () -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Color.White)
            .border(1.dp, CardBorder, shape)
            .clickable(onClick = onClick)
    ) {
        Image(
            painter = painterResource(menu.imageRes),
            contentDescription = menu.title,
            modifier = Modifier
                .fillMaxWidth()
                .height(144.dp),
            contentScale = ContentScale.Crop
        )
        Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            Text(
                text = menu.title,
                style = MaterialTheme.typography.titleMedium,
                color = Maroon
            )
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = menu.ironText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MutedText,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "Lihat Detail →",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = Pink500
                )
            }
            if (menu.warning != null) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = menu.warning,
                    style = MaterialTheme.typography.bodySmall,
                    color = AmberText
                )
            }
        }
    }
}
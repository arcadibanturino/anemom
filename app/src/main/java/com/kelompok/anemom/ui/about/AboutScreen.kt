package com.kelompok.anemom.ui.about

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.GppGood
import androidx.compose.material.icons.outlined.Medication
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.Watch
import androidx.compose.material3.HorizontalDivider
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
import com.kelompok.anemom.ui.components.AnemomCard
import com.kelompok.anemom.ui.components.AnemomTopBar
import com.kelompok.anemom.ui.components.MomAvatar
import com.kelompok.anemom.ui.theme.CardBorder
import com.kelompok.anemom.ui.theme.Maroon
import com.kelompok.anemom.ui.theme.MutedText
import com.kelompok.anemom.ui.theme.Pink100
import com.kelompok.anemom.ui.theme.Pink500

private data class AboutFeature(val icon: ImageVector, val title: String, val description: String)

private val aboutFeatures = listOf(
    AboutFeature(
        Icons.Outlined.Visibility,
        "AI Scan Mata",
        "Analisis foto mata untuk skor skrining awal, bukan nilai Hb."
    ),
    AboutFeature(
        Icons.Outlined.Watch,
        "Smartwatch PPG",
        "Merekam sinyal nadi dan kualitas rekaman, tidak mengukur Hb secara langsung."
    ),
    AboutFeature(
        Icons.Outlined.Description,
        "Hb Laboratorium & riwayat",
        "Catat hasil Hb laboratorium dan pantau perubahannya melalui grafik serta riwayat."
    ),
    AboutFeature(
        Icons.Outlined.Medication,
        "Pelacak TTD & nutrisi",
        "Catat konsumsi Tablet Tambah Darah (TTD) dan temukan rekomendasi nutrisi."
    )
)

/** Halaman "Tentang Anemom" sesuai desain Figma. */
@Composable
fun AboutScreen(onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        AnemomTopBar(
            title = "Tentang Anemom",
            subtitle = "Kenali sahabat sehat Bunda",
            onBack = onBack
        )

        // Kartu identitas aplikasi
        AnemomCard(Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                MomAvatar(size = 64.dp)
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(
                        text = "ANEMOM",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Maroon
                    )
                    Text(
                        text = "Sahabat Bunda, peduli anemia",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = Pink500
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = "Anemom membantu Bunda melakukan skrining dan pemantauan awal anemia selama " +
                    "kehamilan, serta mencatat kebiasaan sehat sehari-hari.",
                style = MaterialTheme.typography.bodySmall,
                color = MutedText
            )
        }

        Spacer(Modifier.height(14.dp))
        HighlightCard(
            title = "Lebih peduli sejak dini",
            body = "Membantu Bunda mengenali indikasi risiko anemia lebih awal dan menyiapkan catatan " +
                "untuk berdiskusi dengan bidan atau dokter."
        )

        Spacer(Modifier.height(20.dp))
        Text(
            text = "Fitur utama Anemom",
            style = MaterialTheme.typography.titleMedium,
            color = Maroon
        )
        Spacer(Modifier.height(10.dp))
        AnemomCard(Modifier.fillMaxWidth()) {
            aboutFeatures.forEachIndexed { index, feature ->
                FeatureRow(feature)
                if (index != aboutFeatures.lastIndex) {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        color = CardBorder
                    )
                }
            }
        }

        Spacer(Modifier.height(20.dp))
        Text(
            text = "Mendampingi langkah sehat Bunda",
            style = MaterialTheme.typography.titleMedium,
            color = Maroon
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "Catatan lebih teratur, kebiasaan konsumsi TTD lebih terpantau, dan informasi " +
                "kesehatan lebih mudah dibagikan saat konsultasi.",
            style = MaterialTheme.typography.bodySmall,
            color = MutedText
        )

        Spacer(Modifier.height(14.dp))
        HighlightCard(
            title = "Skrining awal, bukan diagnosis",
            body = "Hasil AI adalah estimasi skrining awal, bukan diagnosis medis. Anemom tidak " +
                "menggantikan pemeriksaan Hb laboratorium atau konsultasi dengan tenaga kesehatan.",
            icon = Icons.Outlined.GppGood
        )
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun HighlightCard(title: String, body: String, icon: ImageVector? = null) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Pink100.copy(alpha = 0.75f))
            .border(1.dp, CardBorder, RoundedCornerShape(18.dp))
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Maroon,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = Maroon
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = body,
            style = MaterialTheme.typography.bodySmall,
            color = Maroon.copy(alpha = 0.85f)
        )
    }
}

@Composable
private fun FeatureRow(feature: AboutFeature) {
    Row(verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(Pink100, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = feature.icon,
                contentDescription = null,
                tint = Pink500,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = feature.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = Maroon
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = feature.description,
                style = MaterialTheme.typography.bodySmall,
                color = MutedText
            )
        }
    }
}

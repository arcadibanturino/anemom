package com.kelompok.anemom.ui.nutrisi

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
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
import com.kelompok.anemom.data.model.FoodDetail
import com.kelompok.anemom.ui.AppViewModelProvider
import com.kelompok.anemom.ui.components.AnemomCard
import com.kelompok.anemom.ui.components.AnemomOutlinedButton
import com.kelompok.anemom.ui.components.AnemomScreen
import com.kelompok.anemom.ui.theme.AmberBg
import com.kelompok.anemom.ui.theme.AmberText
import com.kelompok.anemom.ui.theme.Maroon
import com.kelompok.anemom.ui.theme.MutedText
import com.kelompok.anemom.ui.theme.Pink100
import com.kelompok.anemom.ui.theme.Pink500

/** Detail Bahan Makanan, dibuka dari "Lihat Detail" di Rekomendasi Makanan. */
@Composable
fun FoodDetailScreen(
    onBack: () -> Unit,
    viewModel: FoodDetailViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    AnemomScreen(
        title = "Detail Bahan Makanan",
        subtitle = null,
        onBack = onBack,
        modifier = Modifier.navigationBarsPadding()
    ) {
        val detail = state.detail
        when {
            state.isLoading -> Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 48.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Pink500)
            }

            detail == null -> {
                Spacer(Modifier.height(16.dp))
                Text(
                    text = state.error ?: "Detail bahan gagal dimuat.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MutedText
                )
                Spacer(Modifier.height(12.dp))
                AnemomOutlinedButton(
                    text = "Coba lagi",
                    onClick = viewModel::load,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            else -> DetailContent(detail)
        }
    }
}

@Composable
private fun DetailContent(detail: FoodDetail) {
    val food = detail.item

    Spacer(Modifier.height(4.dp))
    MenuImage(
        imageUrl = food.imageUrl,
        contentDescription = food.name,
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .clip(RoundedCornerShape(20.dp))
    )

    Spacer(Modifier.height(16.dp))
    food.subcategoryLabel?.let {
        Text(
            text = it.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.8.sp,
            color = Pink500
        )
        Spacer(Modifier.height(6.dp))
    }
    Text(
        text = food.name,
        style = MaterialTheme.typography.headlineSmall.copy(fontSize = 28.sp, lineHeight = 34.sp),
        fontWeight = FontWeight.Bold,
        color = Maroon
    )
    Spacer(Modifier.height(4.dp))
    Text(
        text = "Bahan utama untuk variasi menu bergizi Bunda.",
        style = MaterialTheme.typography.bodyMedium,
        color = MutedText
    )

    food.pregnancyWarning?.let {
        Spacer(Modifier.height(12.dp))
        AnemomCard(Modifier.fillMaxWidth(), containerColor = AmberBg) {
            Text(it, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = AmberText)
        }
    }

    food.keyNutrients?.let {
        Spacer(Modifier.height(16.dp))
        AnemomCard(Modifier.fillMaxWidth(), containerColor = Pink100.copy(alpha = 0.75f)) {
            Text(
                text = it,
                style = MaterialTheme.typography.titleLarge.copy(fontSize = 22.sp, lineHeight = 28.sp),
                fontWeight = FontWeight.SemiBold,
                color = Maroon
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Manfaat gizi, bukan dosis terapi anemia.",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                color = MutedText
            )
        }
    }

    // Bahan tanpa isi detail tetap menampilkan deskripsi singkatnya.
    (food.benefit ?: food.description)?.let {
        Spacer(Modifier.height(14.dp))
        SectionCard(title = "Manfaat Nutrisi", body = it)
    }
    food.preparation?.let {
        Spacer(Modifier.height(14.dp))
        SectionCard(title = "Cara memilih & menyiapkan", body = it)
    }

    Spacer(Modifier.height(14.dp))
    SectionCard(
        title = "Sesuai risiko & alergi",
        body = listOfNotNull(food.allergyNote, FoodGuidance.detailRiskSentence(detail.context)).joinToString(" "),
        containerColor = Pink100.copy(alpha = 0.75f)
    )

    Spacer(Modifier.height(16.dp))
    Text(
        text = "Makanan bukan pengganti TTD, tes Hb, atau konsultasi medis.",
        style = MaterialTheme.typography.bodySmall,
        color = Maroon
    )
}

@Composable
private fun SectionCard(title: String, body: String, containerColor: Color = Color.White) {
    AnemomCard(Modifier.fillMaxWidth(), containerColor = containerColor) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Maroon
        )
        Spacer(Modifier.height(8.dp))
        Text(text = body, style = MaterialTheme.typography.bodyMedium, color = MutedText)
    }
}

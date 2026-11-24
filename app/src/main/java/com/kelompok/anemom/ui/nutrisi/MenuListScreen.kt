package com.kelompok.anemom.ui.nutrisi

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kelompok.anemom.data.model.FoodGroup
import com.kelompok.anemom.data.model.FoodItem
import com.kelompok.anemom.ui.AppViewModelProvider
import com.kelompok.anemom.ui.components.AnemomCard
import com.kelompok.anemom.ui.components.AnemomOutlinedButton
import com.kelompok.anemom.ui.components.AnemomScreen
import com.kelompok.anemom.ui.theme.AmberText
import com.kelompok.anemom.ui.theme.CardBorder
import com.kelompok.anemom.ui.theme.Maroon
import com.kelompok.anemom.ui.theme.MutedText
import com.kelompok.anemom.ui.theme.Pink100
import com.kelompok.anemom.ui.theme.Pink500

/**
 * Rekomendasi Makanan (dibuka dari "Lihat Semua" di Beranda): bahan bergizi per tab Sayur / Buah /
 * Protein, disesuaikan dengan risiko Hb dan alergi Bunda. Bahan dengan detail bisa dibuka.
 */
@Composable
fun MenuListScreen(
    onBack: () -> Unit,
    onOpenDetail: (foodId: Long) -> Unit,
    viewModel: MenuListViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    AnemomScreen(
        title = "Rekomendasi Makanan",
        subtitle = null,
        onBack = onBack,
        modifier = Modifier.navigationBarsPadding()
    ) {
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Bahan Bergizi Bunda",
            style = MaterialTheme.typography.headlineSmall.copy(fontSize = 26.sp, lineHeight = 32.sp),
            fontWeight = FontWeight.Bold,
            color = Maroon
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "Pilih bahan utama, bukan hidangan jadi.",
            style = MaterialTheme.typography.bodyMedium,
            color = MutedText
        )

        Spacer(Modifier.height(16.dp))
        if (!state.isLoading && state.error == null) {
            ContextCard(state)
            Spacer(Modifier.height(16.dp))
        }

        GroupTabs(selected = state.selectedGroup, onSelect = viewModel::selectGroup)

        Spacer(Modifier.height(20.dp))
        when {
            state.isLoading -> Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Pink500)
            }

            state.error != null -> {
                Text(state.error.orEmpty(), style = MaterialTheme.typography.bodyMedium, color = MutedText)
                Spacer(Modifier.height(12.dp))
                AnemomOutlinedButton(
                    text = "Coba lagi",
                    onClick = viewModel::retry,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            else -> {
                val items = state.visibleItems
                Text(
                    text = "${state.selectedGroup.label} · ${items.size} pilihan",
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp),
                    fontWeight = FontWeight.Bold,
                    color = Maroon
                )
                Spacer(Modifier.height(12.dp))
                if (items.isEmpty()) {
                    Text(
                        text = "Belum ada bahan di kelompok ini.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MutedText
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        items.forEach { food ->
                            if (food.group == FoodGroup.BUAH) {
                                CompactFoodCard(food, onOpenDetail)
                            } else {
                                LargeFoodCard(food, onOpenDetail)
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(20.dp))
        DisclaimerCard()
    }
}

/** Kartu "Sesuai kebutuhan Bunda": risiko Hb terakhir dan alergi dari profil. */
@Composable
private fun ContextCard(state: MenuListUiState) {
    val ctx = state.context
    AnemomCard(Modifier.fillMaxWidth(), containerColor = Pink100.copy(alpha = 0.75f)) {
        Text(
            text = "Sesuai kebutuhan Bunda",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = Maroon
        )
        Spacer(Modifier.height(6.dp))
        Text(FoodGuidance.summaryLine(ctx), style = MaterialTheme.typography.bodySmall, color = Maroon)
        Text(FoodGuidance.mainLine(ctx), style = MaterialTheme.typography.bodySmall, color = Maroon)
        Spacer(Modifier.height(6.dp))
        Text(
            text = FoodGuidance.detailLine(ctx),
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, lineHeight = 15.sp),
            color = MutedText
        )
    }
}

@Composable
private fun GroupTabs(selected: FoodGroup, onSelect: (FoodGroup) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        MENU_TABS.forEach { group ->
            val isSelected = group == selected
            val shape = RoundedCornerShape(50)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(42.dp)
                    .clip(shape)
                    .background(if (isSelected) Pink500 else Color.White)
                    .border(1.dp, if (isSelected) Pink500 else CardBorder, shape)
                    .clickable(role = Role.Tab) { onSelect(group) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = group.label,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                    color = if (isSelected) Color.White else Pink500
                )
            }
        }
    }
}

/** Kartu besar (Sayur, Protein): foto di atas, teks di bawah. */
@Composable
private fun LargeFoodCard(food: FoodItem, onOpenDetail: (Long) -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Color.White)
            .border(1.dp, CardBorder, shape)
            .then(if (food.hasDetail) Modifier.clickable { onOpenDetail(food.id) } else Modifier)
    ) {
        MenuImage(
            imageUrl = food.imageUrl,
            contentDescription = food.name,
            modifier = Modifier
                .fillMaxWidth()
                .height(148.dp)
        )
        Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            FoodTexts(food)
        }
    }
}

/** Kartu ringkas (Buah): foto kecil di kiri. */
@Composable
private fun CompactFoodCard(food: FoodItem, onOpenDetail: (Long) -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Color.White)
            .border(1.dp, CardBorder, shape)
            .then(if (food.hasDetail) Modifier.clickable { onOpenDetail(food.id) } else Modifier)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        MenuImage(
            imageUrl = food.imageUrl,
            contentDescription = food.name,
            modifier = Modifier
                .size(84.dp)
                .clip(RoundedCornerShape(14.dp))
        )
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            FoodTexts(food)
        }
    }
}

@Composable
private fun FoodTexts(food: FoodItem) {
    Text(
        text = food.name,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = Maroon
    )
    if (food.description != null) {
        Spacer(Modifier.height(4.dp))
        Text(food.description, style = MaterialTheme.typography.bodySmall, color = MutedText)
    }
    if (food.pregnancyWarning != null) {
        Spacer(Modifier.height(6.dp))
        Text(food.pregnancyWarning, style = MaterialTheme.typography.bodySmall, color = AmberText)
    }
    if (food.hasDetail) {
        Spacer(Modifier.height(6.dp))
        Text(
            text = "Lihat Detail →",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = Pink500
        )
    }
}

@Composable
internal fun DisclaimerCard(modifier: Modifier = Modifier) {
    AnemomCard(modifier.fillMaxWidth(), containerColor = Pink100.copy(alpha = 0.75f)) {
        Text(
            text = "Makanan bukan pengganti TTD, tes Hb, atau konsultasi medis.",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = Maroon
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Sesuaikan porsi dan kebutuhan dengan anjuran bidan atau dokter. " +
                "Cuci bahan segar; masak protein hingga matang.",
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, lineHeight = 15.sp),
            color = MutedText
        )
    }
}

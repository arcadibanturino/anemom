package com.kelompok.anemom.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Medication
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kelompok.anemom.R
import com.kelompok.anemom.data.model.DashboardData
import com.kelompok.anemom.ui.AppViewModelProvider
import com.kelompok.anemom.ui.components.AnemomCard
import com.kelompok.anemom.ui.components.AnemomChip
import com.kelompok.anemom.ui.components.RiskChip
import com.kelompok.anemom.ui.theme.AmberText
import com.kelompok.anemom.ui.theme.CardBorder
import com.kelompok.anemom.ui.theme.Maroon
import com.kelompok.anemom.ui.theme.MutedText
import com.kelompok.anemom.ui.theme.Pink100
import com.kelompok.anemom.ui.theme.Pink500
import com.kelompok.anemom.util.DateFormats
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material3.CircularProgressIndicator
import com.kelompok.anemom.ui.components.AnemomOutlinedButton
import com.kelompok.anemom.ui.nutrisi.MenuImage
import com.kelompok.anemom.ui.nutrisi.MenuRecommendation

/**
 * Beranda / dashboard.
 * [onFeatureClick] dipanggil saat salah satu fitur ditekan (sementara membuka halaman TODO).
 */
@Composable
fun HomeScreen(
    onFeatureClick: (featureName: String) -> Unit,
    viewModel: HomeViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val data by viewModel.dashboard.collectAsStateWithLifecycle()
    val error by viewModel.loadError.collectAsStateWithLifecycle()
    val menus by viewModel.menus.collectAsStateWithLifecycle()

    val dashboard = data
    if (dashboard != null) {
        HomeContent(dashboard = dashboard, menus = menus, onFeatureClick = onFeatureClick)
    } else {
        HomeLoading(error = error, onRetry = viewModel::retry)
    }
}

/** Tampil selama data Beranda pertama belum datang dari server; jika gagal ada tombol ulangi. */
@Composable
private fun HomeLoading(error: String?, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (error == null) {
            CircularProgressIndicator(color = Pink500)
            Spacer(Modifier.height(12.dp))
            Text(
                text = "Memuat data Bunda...",
                style = MaterialTheme.typography.bodyMedium,
                color = MutedText
            )
        } else {
            Text(
                text = "Beranda belum bisa dimuat",
                style = MaterialTheme.typography.titleMedium,
                color = Maroon,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = error,
                style = MaterialTheme.typography.bodySmall,
                color = MutedText,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(16.dp))
            AnemomOutlinedButton(text = "Coba lagi", onClick = onRetry)
        }
    }
}

@Composable
private fun HomeContent(
    dashboard: DashboardData,
    menus: List<MenuRecommendation>,
    onFeatureClick: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Sapaan
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = if (dashboard.motherName.isBlank()) "Halo, Bunda" else "Halo, Bunda ${dashboard.motherName}",
                    style = MaterialTheme.typography.titleLarge,
                    color = Maroon
                )
                Text(
                    text = dashboard.todayText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MutedText
                )
            }
            Icon(
                imageVector = Icons.Outlined.FavoriteBorder,
                contentDescription = null,
                tint = Pink500
            )
        }

        PregnancyBanner(dashboard)
        HbCard(dashboard)
        TtdCard(dashboard)

        MenuRecommendationSection(
            menus = menus,
            onSeeAll = { onFeatureClick("Rekomendasi Menu Makanan") }
        )

        Text(
            text = "AI/PPG hanya skrining awal, bukan tes darah.\nKonfirmasi dengan pemeriksaan Hb darah.",
            style = MaterialTheme.typography.bodySmall,
            color = MutedText,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        TrendCard(dashboard)
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun PregnancyBanner(dashboard: DashboardData) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Pink100)
            .padding(18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = "Hari yang baik untuk sehat ♡",
                style = MaterialTheme.typography.bodyMedium,
                color = MutedText
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "${dashboard.pregnancyWeek} minggu",
                fontSize = 34.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Maroon
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "${dashboard.trimesterLabel} · HPL ${dashboard.dueDateText}",
                style = MaterialTheme.typography.bodySmall,
                color = MutedText
            )
        }
        Box(
            modifier = Modifier
                .size(104.dp)
                .clip(CircleShape)
                .background(Color(0xFFF2D8E1)),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(R.drawable.img_banner_mom),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxHeight(0.82f)
                    .padding(start = 4.dp),
                contentScale = ContentScale.Fit
            )
        }
    }
}

@Composable
private fun HbCard(dashboard: DashboardData) {
    AnemomCard(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Hb darah terakhir",
                style = MaterialTheme.typography.titleMedium,
                color = Maroon,
                modifier = Modifier.weight(1f)
            )
            AnemomChip(
                text = "Hasil laboratorium",
                containerColor = Pink100,
                contentColor = Pink500
            )
        }
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = dashboard.lastHb?.let { DateFormats.hb(it) } ?: "-",
                    fontSize = 40.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Maroon
                )
                Text(
                    text = " g/dL",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MutedText,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            dashboard.risk?.let { RiskChip(risk = it) }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = dashboard.lastHbNote,
            style = MaterialTheme.typography.bodySmall,
            color = MutedText
        )
    }
}

@Composable
private fun TtdCard(dashboard: DashboardData) {
    AnemomCard(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.Medication,
                contentDescription = null,
                tint = Pink500,
                modifier = Modifier.size(22.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Kepatuhan TTD",
                style = MaterialTheme.typography.titleMedium,
                color = Maroon,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "${dashboard.ttdPercent}%",
                style = MaterialTheme.typography.titleMedium,
                color = Pink500
            )
        }
        Spacer(Modifier.height(10.dp))
        // Bar progres sederhana
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(CircleShape)
                .background(Pink100)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(dashboard.ttdPercent.coerceIn(0, 100) / 100f)
                    .fillMaxHeight()
                    .clip(CircleShape)
                    .background(Pink500)
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = dashboard.ttdSummary,
            style = MaterialTheme.typography.bodySmall,
            color = MutedText
        )
    }
}

@Composable
private fun MenuRecommendationSection(
    menus: List<MenuRecommendation>,
    onSeeAll: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Rekomendasi Menu Makanan",
                style = MaterialTheme.typography.titleMedium,
                color = Maroon,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "Lihat Semua",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = Pink500,
                modifier = Modifier
                    .clickable(onClick = onSeeAll)
                    .padding(vertical = 4.dp)
            )
        }
        Text(
            text = "Pilihan makanan bergizi untuk membantu memenuhi kebutuhan nutrisi Bunda.",
            style = MaterialTheme.typography.bodySmall,
            color = MutedText
        )

        val shown = menus.filter { it.warning == null }
        if (shown.isEmpty()) {
            Text(
                text = "Rekomendasi makanan belum tersedia.",
                style = MaterialTheme.typography.bodySmall,
                color = MutedText
            )
        } else {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(shown, key = { it.title }) { menu -> MenuCard(menu = menu) }
            }
            Text(
                text = "Geser untuk melihat bahan lainnya →",
                style = MaterialTheme.typography.bodySmall,
                color = MutedText
            )
        }
    }
}

@Composable
private fun MenuCard(menu: MenuRecommendation) {
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier = Modifier
            .width(240.dp)
            .clip(shape)
            .background(Color.White)
            .border(1.dp, CardBorder, shape)
    ) {
        MenuImage(
            imageUrl = menu.imageUrl,
            contentDescription = menu.title,
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
        )
        Column(Modifier.padding(14.dp)) {
            Text(
                text = menu.title,
                style = MaterialTheme.typography.titleMedium,
                color = Maroon,
                maxLines = 1
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = menu.tag,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = Pink500,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = menu.description.orEmpty(),
                style = MaterialTheme.typography.bodySmall,
                color = MutedText,
                minLines = 3,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun TrendCard(dashboard: DashboardData) {
    AnemomCard(Modifier.fillMaxWidth()) {
        Text(
            text = "Tren HB",
            style = MaterialTheme.typography.titleMedium,
            color = Maroon
        )
        Spacer(Modifier.height(8.dp))
        if (dashboard.hbTrend.size >= 2) {
            HbTrendChart(points = dashboard.hbTrend)
            Spacer(Modifier.height(4.dp))
        }
        Text(
            text = dashboard.hbTrendNote,
            style = MaterialTheme.typography.bodySmall,
            color = AmberText
        )
    }
}

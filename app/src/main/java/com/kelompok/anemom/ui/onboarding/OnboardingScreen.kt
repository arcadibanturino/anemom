package com.kelompok.anemom.ui.onboarding

import androidx.annotation.DrawableRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kelompok.anemom.R
import com.kelompok.anemom.ui.AppViewModelProvider
import com.kelompok.anemom.ui.components.GradientButton
import com.kelompok.anemom.ui.theme.CardBorder
import com.kelompok.anemom.ui.theme.Maroon
import com.kelompok.anemom.ui.theme.MutedText
import com.kelompok.anemom.ui.theme.Pink100
import com.kelompok.anemom.ui.theme.Pink500
import kotlinx.coroutines.launch

private data class OnboardingPage(
    @DrawableRes val image: Int,
    val chip: String,
    val title: String,
    val description: String
)

private val onboardingPages = listOf(
    OnboardingPage(
        image = R.drawable.img_onb_1,
        chip = "TEMANI KEHAMILAN BUNDA",
        title = "Pantau anemia,\njaga dua kehidupan",
        description = "Kenali risiko anemia sejak dini. Catat Hb darah dan pantau kesehatan Bunda selama kehamilan."
    ),
    OnboardingPage(
        image = R.drawable.img_onb_2,
        chip = "TEKNOLOGI YANG PEDULI",
        title = "Lebih dekat dengan\nkesehatan Bunda",
        description = "AI Scan Mata dan sinyal PPG smartwatch membantu skrining awal. Bukan pengganti tes Hb darah."
    ),
    OnboardingPage(
        image = R.drawable.img_onb_3,
        chip = "LANGKAH KECIL, DAMPAK BESAR",
        title = "Nutrisi terpenuhi,\nBunda lebih berenergi",
        description = "Temukan makanan kaya zat besi dan ingat jadwal Tablet Tambah Darah (TTD) setiap hari."
    )
)

@Composable
fun OnboardingScreen(
    onFinished: () -> Unit,
    viewModel: OnboardingViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val pagerState = rememberPagerState(pageCount = { onboardingPages.size })
    val scope = rememberCoroutineScope()
    val isLastPage = pagerState.currentPage == onboardingPages.lastIndex

    val finish = { viewModel.finishOnboarding(onDone = onFinished) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp)
    ) {
        // Header: nama aplikasi + tombol Lewati
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Anemom",
                style = MaterialTheme.typography.titleLarge,
                color = Maroon
            )
            Text(
                text = "Lewati",
                style = MaterialTheme.typography.bodyMedium,
                color = MutedText,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { finish() }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) { index ->
            OnboardingPageContent(onboardingPages[index])
        }

        // Indikator halaman
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(onboardingPages.size) { index ->
                val selected = pagerState.currentPage == index
                val width = animateDpAsState(if (selected) 28.dp else 8.dp, label = "dotWidth")
                val color = animateColorAsState(if (selected) Pink500 else CardBorder, label = "dotColor")
                Box(
                    Modifier
                        .padding(horizontal = 4.dp)
                        .height(8.dp)
                        .width(width.value)
                        .clip(CircleShape)
                        .background(color.value)
                )
            }
        }

        GradientButton(
            text = if (isLastPage) "Mulai bersama Anemom" else "Lanjut",
            onClick = {
                if (isLastPage) {
                    finish()
                } else {
                    scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                }
            }
        )
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun OnboardingPageContent(page: OnboardingPage) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(page.image),
            contentDescription = null,
            modifier = Modifier.height(240.dp),
            contentScale = ContentScale.Fit
        )
        Spacer(Modifier.height(32.dp))
        Text(
            text = page.chip,
            color = Pink500,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp,
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(Pink100)
                .padding(horizontal = 14.dp, vertical = 7.dp)
        )
        Spacer(Modifier.height(18.dp))
        Text(
            text = page.title,
            style = MaterialTheme.typography.headlineMedium,
            color = Maroon,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(14.dp))
        Text(
            text = page.description,
            style = MaterialTheme.typography.bodyMedium,
            color = MutedText,
            textAlign = TextAlign.Center
        )
    }
}

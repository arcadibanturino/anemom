package com.kelompok.anemom.ui.splash

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kelompok.anemom.R
import com.kelompok.anemom.ui.AppViewModelProvider
import com.kelompok.anemom.ui.theme.MutedText
import com.kelompok.anemom.ui.theme.Pink100
import com.kelompok.anemom.ui.theme.Pink500
import kotlinx.coroutines.delay

private const val SPLASH_DURATION_MS = 2200L

@Composable
fun SplashScreen(
    onFinished: (nextRoute: String) -> Unit,
    viewModel: SplashViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    // Titik loading bergerak selama splash tampil.
    var activeDot by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(400)
            activeDot = (activeDot + 1) % 3
        }
    }

    LaunchedEffect(Unit) {
        delay(SPLASH_DURATION_MS)
        onFinished(viewModel.resolveStartRoute())
    }

    Box(Modifier.fillMaxSize()) {
        // Latar: lingkaran besar di atas dan dua gelombang di bawah.
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(
                color = Pink100,
                radius = size.width * 0.57f,
                center = Offset(size.width / 2f, size.height * 0.345f)
            )
            drawOval(
                color = Pink100.copy(alpha = 0.55f),
                topLeft = Offset(-size.width * 0.15f, size.height * 0.84f),
                size = Size(size.width * 1.3f, size.height * 0.4f)
            )
            drawOval(
                color = Pink100.copy(alpha = 0.9f),
                topLeft = Offset(size.width * 0.3f, size.height * 0.91f),
                size = Size(size.width * 1.0f, size.height * 0.3f)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.weight(0.9f))
            Image(
                painter = painterResource(R.drawable.img_logo_splash),
                contentDescription = "Logo Anemom",
                modifier = Modifier.width(250.dp),
                contentScale = ContentScale.Fit
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = "Skrining anemia untuk ibu hamil",
                style = MaterialTheme.typography.bodyLarge,
                color = MutedText,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                repeat(3) { index ->
                    Box(
                        Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(if (index == activeDot) Pink500 else Pink500.copy(alpha = 0.3f))
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = "Memuat aplikasi...",
                style = MaterialTheme.typography.bodyMedium,
                color = MutedText
            )
            Spacer(Modifier.height(40.dp))
        }
    }
}

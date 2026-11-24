package com.kelompok.anemom.alarm

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardDoubleArrowLeft
import androidx.compose.material.icons.filled.KeyboardDoubleArrowRight
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.Medication
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kelompok.anemom.data.repository.TtdSummary
import com.kelompok.anemom.ui.theme.Maroon
import com.kelompok.anemom.ui.theme.MutedText
import com.kelompok.anemom.ui.theme.Pink100
import com.kelompok.anemom.ui.theme.Pink50
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.time.ZonedDateTime
import kotlin.math.roundToInt

private val AlarmPinkTop = Color(0xFFF48FB1)
private val AlarmPinkBottom = Color(0xFFE5598A)
private val DeepPink = Color(0xFFD81B60)

@Composable
fun AlarmFlow(
    state: AlarmUiState,
    modeLabel: String?,
    onTaken: () -> Unit,
    onNotTaken: () -> Unit,
    onOpenSnooze: () -> Unit,
    onSnooze: (Int) -> Unit,
    onBackToAlarm: () -> Unit,
    onOpenTracker: () -> Unit,
    onClose: () -> Unit
) {
    when (state.step) {
        AlarmStep.RINGING -> RingingScreen(state, modeLabel, onTaken, onNotTaken, onOpenSnooze)
        AlarmStep.SNOOZE_PICK -> {
            BackHandler(onBack = onBackToAlarm)
            SnoozePickScreen(state.reportedNotTaken, onSnooze, onBackToAlarm)
        }
        AlarmStep.SNOOZED -> {
            BackHandler(onBack = onClose)
            SnoozedScreen(state, onOpenTracker, onBackToAlarm)
        }
        AlarmStep.DONE -> {
            BackHandler(onBack = onClose)
            DoneScreen(state, onOpenTracker, onClose)
        }
    }
}

// ---------------------------------------------------------------- Berbunyi

@Composable
private fun RingingScreen(
    state: AlarmUiState,
    modeLabel: String?,
    onTaken: () -> Unit,
    onNotTaken: () -> Unit,
    onOpenSnooze: () -> Unit
) {
    var now by remember { mutableStateOf(ZonedDateTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(10_000)
            now = ZonedDateTime.now()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(AlarmPinkTop, AlarmPinkBottom)))
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(32.dp))
        if (modeLabel != null) {
            Text(
                text = modeLabel,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = DeepPink,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Color.White.copy(alpha = 0.85f))
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            )
            Spacer(Modifier.height(12.dp))
        }
        Text(AlarmTimeText.dayLong(now), style = MaterialTheme.typography.bodyLarge, color = Color.White)
        Text(
            text = AlarmTimeText.time(now.toLocalTime()),
            fontSize = 76.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            lineHeight = 84.sp
        )
        Spacer(Modifier.height(12.dp))
        Box(
            modifier = Modifier
                .size(110.dp)
                .clip(RoundedCornerShape(36.dp))
                .background(Color.White.copy(alpha = 0.35f)),
            contentAlignment = Alignment.Center
        ) {
            Box(
                Modifier
                    .size(76.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.Medication, contentDescription = null, tint = DeepPink, modifier = Modifier.size(44.dp))
            }
        }
        Spacer(Modifier.height(20.dp))
        Text(
            text = "Waktunya minum TTD, Bunda!",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = "Minum TTD secara rutin membantu mencegah anemia selama kehamilan. " +
                "Ikuti dosis dari bidan atau dokter, lalu konfirmasi setelah benar-benar diminum.",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.95f),
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(40.dp))
        if (state.isSaving) {
            CircularProgressIndicator(color = Color.White)
            Spacer(Modifier.height(8.dp))
            Text("Menyimpan...", color = Color.White, style = MaterialTheme.typography.bodySmall)
        } else {
            ConfirmSlider(onLeft = onNotTaken, onRight = onTaken)
        }
        state.error?.let {
            Spacer(Modifier.height(10.dp))
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Maroon.copy(alpha = 0.55f))
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            )
        }
        Spacer(Modifier.height(14.dp))
        Text(
            text = "Geser ke kiri jika Belum Diminum\natau ke kanan jika Sudah Diminum",
            style = MaterialTheme.typography.bodySmall,
            color = Color.White,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(18.dp))
        Button(
            onClick = onOpenSnooze,
            colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.9f), contentColor = DeepPink),
            shape = RoundedCornerShape(50)
        ) {
            Icon(Icons.Outlined.Schedule, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Tunda Pengingat", fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(32.dp))
    }
}

/**
 * Geser tombol tengah: ke kanan = Sudah Diminum, ke kiri = Belum Diminum.
 * Butuh geseran lebih dari separuh supaya tidak terkonfirmasi tanpa sengaja.
 * Pembaca layar mendapat dua aksi khusus.
 */
@Composable
private fun ConfirmSlider(onLeft: () -> Unit, onRight: () -> Unit) {
    val scope = rememberCoroutineScope()
    val offset = remember { Animatable(0f) }
    val thumbSize = 76.dp

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(thumbSize)
            .semantics {
                contentDescription = "Geser konfirmasi minum TTD"
                customActions = listOf(
                    CustomAccessibilityAction("Sudah diminum") { onRight(); true },
                    CustomAccessibilityAction("Belum diminum") { onLeft(); true }
                )
            }
    ) {
        val density = LocalDensity.current
        val thumbPx = with(density) { thumbSize.toPx() }
        val maxOffset = (constraints.maxWidth - thumbPx) / 2f

        // Lintasan: kiri (Belum) putih, kanan (Sudah) pink tua.
        Row(
            Modifier
                .fillMaxSize()
                .padding(vertical = 8.dp)
                .clip(RoundedCornerShape(50))
                .border(2.dp, Color.White.copy(alpha = 0.7f), RoundedCornerShape(50))
        ) {
            Row(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(Color.White.copy(alpha = 0.92f))
                    .padding(start = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.KeyboardDoubleArrowLeft, contentDescription = null, tint = DeepPink)
                Text(
                    text = "Belum\nDiminum",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    lineHeight = 17.sp,
                    color = DeepPink,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(start = 4.dp, end = thumbSize / 2)
                )
            }
            Row(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(DeepPink)
                    .padding(end = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    text = "Sudah\nDiminum",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    lineHeight = 17.sp,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(start = thumbSize / 2, end = 4.dp)
                )
                Icon(Icons.Filled.KeyboardDoubleArrowRight, contentDescription = null, tint = Color.White)
            }
        }

        // Tombol tengah yang digeser.
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .offset { IntOffset(offset.value.roundToInt(), 0) }
                .size(thumbSize)
                .shadow(8.dp, CircleShape)
                .clip(CircleShape)
                .background(Color.White)
                .border(4.dp, Pink100, CircleShape)
                .pointerInput(maxOffset) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            val v = offset.value
                            scope.launch {
                                when {
                                    v > maxOffset * 0.6f -> {
                                        offset.animateTo(maxOffset)
                                        onRight()
                                        offset.snapTo(0f)
                                    }
                                    v < -maxOffset * 0.6f -> {
                                        offset.animateTo(-maxOffset)
                                        onLeft()
                                        offset.snapTo(0f)
                                    }
                                    else -> offset.animateTo(0f)
                                }
                            }
                        },
                        onDragCancel = { scope.launch { offset.animateTo(0f) } }
                    ) { change, dragAmount ->
                        change.consume()
                        scope.launch { offset.snapTo((offset.value + dragAmount).coerceIn(-maxOffset, maxOffset)) }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Outlined.Alarm, contentDescription = null, tint = DeepPink, modifier = Modifier.size(34.dp))
        }
    }
}

// ---------------------------------------------------------------- Tunda

@Composable
private fun SnoozePickScreen(reportedNotTaken: Boolean, onSnooze: (Int) -> Unit, onBack: () -> Unit) {
    LightPage {
        Spacer(Modifier.height(48.dp))
        Text("Tunda alarm TTD", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Maroon, textAlign = TextAlign.Center)
        Spacer(Modifier.height(24.dp))
        Text(
            text = if (reportedNotTaken) "Bunda belum minum TTD" else "Ingatkan Bunda lagi nanti",
            fontSize = 19.sp,
            fontWeight = FontWeight.SemiBold,
            color = Maroon,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(24.dp))
        Text(
            text = "Pilih kapan ingin diingatkan kembali.\nMenunda tidak mencatat konsumsi.",
            style = MaterialTheme.typography.bodyMedium,
            color = MutedText,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(36.dp))
        listOf(10, 15, 30).forEach { minutes ->
            PinkButton("Tunda $minutes menit") { onSnooze(minutes) }
            Spacer(Modifier.height(16.dp))
        }
        Spacer(Modifier.height(32.dp))
        InfoBox(
            title = if (reportedNotTaken) "Belum diminum · Tidak menambah kepatuhan" else "Belum dikonfirmasi",
            body = "Kalender tidak ditandai Sudah. Grafik hanya bertambah setelah Bunda mengonfirmasi konsumsi."
        )
        Spacer(Modifier.height(40.dp))
        WhiteButton("Kembali ke alarm", onBack)
    }
}

@Composable
private fun SnoozedScreen(state: AlarmUiState, onOpenTracker: () -> Unit, onBackToAlarm: () -> Unit) {
    val until = state.snoozedUntil ?: ZonedDateTime.now()
    LightPage {
        Spacer(Modifier.height(72.dp))
        Text("Alarm ditunda", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Maroon)
        Spacer(Modifier.height(32.dp))
        Text(
            text = AlarmTimeText.timeWithZone(until.toLocalTime()),
            fontSize = 44.sp,
            fontWeight = FontWeight.Bold,
            color = DeepPink
        )
        Spacer(Modifier.height(16.dp))
        Text("Pengingat berikutnya · ${AlarmTimeText.dateShort(until)}", style = MaterialTheme.typography.bodyMedium, color = MutedText)
        Spacer(Modifier.height(48.dp))
        InfoBox(
            title = if (state.reportedNotTaken) "Belum Diminum" else "Belum Dikonfirmasi",
            body = "Konsumsi belum dicatat.\n" +
                summaryLine(state.summary, prefix = "Jadwal terpenuhi tetap") +
                "Kalender tidak ditandai Sudah."
        )
        Spacer(Modifier.height(80.dp))
        PinkButton("Lihat Pelacak TTD", onOpenTracker)
        Spacer(Modifier.height(16.dp))
        WhiteButton("Kembali ke alarm", onBackToAlarm)
    }
}

@Composable
private fun DoneScreen(state: AlarmUiState, onOpenTracker: () -> Unit, onClose: () -> Unit) {
    LightPage {
        Spacer(Modifier.height(72.dp))
        Text("TTD tercatat", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Maroon)
        Spacer(Modifier.height(32.dp))
        Text(
            text = AlarmTimeText.timeWithZone(state.takenAt ?: LocalTime.now()),
            fontSize = 44.sp,
            fontWeight = FontWeight.Bold,
            color = DeepPink
        )
        Spacer(Modifier.height(16.dp))
        Text("Terima kasih sudah minum TTD hari ini, Bunda.", style = MaterialTheme.typography.bodyMedium, color = MutedText, textAlign = TextAlign.Center)
        Spacer(Modifier.height(48.dp))
        InfoBox(
            title = "Sudah Diminum",
            body = "Kalender ditandai Sudah.\n" + summaryLine(state.summary, prefix = "Jadwal terpenuhi") +
                "Alarm berikutnya besok pada jam yang sama."
        )
        Spacer(Modifier.height(80.dp))
        PinkButton("Lihat Pelacak TTD", onOpenTracker)
        Spacer(Modifier.height(16.dp))
        WhiteButton("Tutup", onClose)
    }
}

private fun summaryLine(summary: TtdSummary?, prefix: String): String =
    if (summary == null) "" else "$prefix ${summary.takenLast7Days}/7 (${summary.percentLast7Days}%).\n"

// ---------------------------------------------------------------- Komponen

@Composable
private fun LightPage(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Pink50)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        content()
        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun InfoBox(title: String, body: String) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Pink100)
            .padding(18.dp)
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Maroon)
        Spacer(Modifier.height(8.dp))
        Text(body, style = MaterialTheme.typography.bodyMedium, color = MutedText)
    }
}

@Composable
private fun PinkButton(text: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        colors = ButtonDefaults.buttonColors(containerColor = DeepPink, contentColor = Color.White),
        shape = RoundedCornerShape(50)
    ) {
        Text(text, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
    }
}

@Composable
private fun WhiteButton(text: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = DeepPink),
        shape = RoundedCornerShape(50)
    ) {
        Text(text, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
    }
}


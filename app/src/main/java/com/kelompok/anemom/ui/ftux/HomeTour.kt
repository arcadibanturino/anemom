package com.kelompok.anemom.ui.ftux

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kelompok.anemom.ui.theme.Maroon
import com.kelompok.anemom.ui.theme.MutedText
import com.kelompok.anemom.ui.theme.Pink100
import com.kelompok.anemom.ui.theme.Pink500

/** Bagian Beranda yang disorot panduan pertama kali (FTUX). */
enum class TourTarget { DASHBOARD, SKRINING, TREN_HB, TTD, MENU }

internal data class TourKey(val target: TourTarget, val part: Int)

private data class TourStep(val target: TourTarget, val title: String, val body: String)

private val steps = listOf(
    TourStep(TourTarget.DASHBOARD, "Dashboard", "Ringkasan kesehatan Bunda. Lihat usia kehamilan, Hb terakhir, dan kepatuhan TTD di sini."),
    TourStep(TourTarget.SKRINING, "Skrining", "Pemeriksaan awal risiko anemia. Mulai skrining dari menu ini, lalu ikuti petunjuknya."),
    TourStep(TourTarget.TREN_HB, "Tren Hb", "Memantau perkembangan Hb. Bandingkan hasil pemeriksaan Bunda dari waktu ke waktu."),
    TourStep(TourTarget.TTD, "Pengingat TTD", "Memantau konsumsi tablet tambah darah. Lihat kepatuhan dan jadwal TTD berikutnya."),
    TourStep(TourTarget.MENU, "Rekomendasi Makanan", "Pilihan bahan bergizi. Temukan inspirasi makanan untuk membantu memenuhi nutrisi Bunda.")
)

/**
 * Status panduan Beranda. Tampil sekali per HP; setelah "Selesai" atau "Lewati" tidak muncul lagi.
 * Posisi tiap bagian dicatat oleh [tourTarget] (koordinat layar), lalu [HomeTourOverlay] melubanginya.
 */
@Stable
class HomeTourController(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("anemom_ftux", Context.MODE_PRIVATE)

    var stepIndex by mutableStateOf<Int?>(null)
        private set

    /** (bagian, potongan) -> posisi di layar. Satu bagian bisa terdiri dari beberapa kartu. */
    internal val bounds = mutableStateMapOf<TourKey, Rect>()

    val isDone: Boolean get() = prefs.getBoolean(KEY_DONE, false)

    val activeTarget: TourTarget? get() = stepIndex?.let { steps[it].target }

    fun isReady(target: TourTarget): Boolean = bounds.keys.any { it.target == target }

    fun start() {
        if (!isDone && stepIndex == null) stepIndex = 0
    }

    fun next() {
        val i = stepIndex ?: return
        if (i >= steps.lastIndex) finish() else stepIndex = i + 1
    }

    fun back() {
        val i = stepIndex ?: return
        if (i > 0) stepIndex = i - 1
    }

    fun finish() {
        prefs.edit().putBoolean(KEY_DONE, true).apply()
        stepIndex = null
    }

    /** Untuk menampilkan panduan lagi (mis. dari menu bantuan). */
    fun reset() {
        prefs.edit().putBoolean(KEY_DONE, false).apply()
    }

    internal fun rectOf(target: TourTarget): Rect? =
        bounds.filterKeys { it.target == target }.values.reduceOrNull { a, b -> a.union(b) }

    private companion object {
        const val KEY_DONE = "home_tour_done_v1"
    }
}

private fun Rect.union(other: Rect) = Rect(
    minOf(left, other.left), minOf(top, other.top), maxOf(right, other.right), maxOf(bottom, other.bottom)
)

val LocalHomeTour = staticCompositionLocalOf<HomeTourController?> { null }

/**
 * Tandai bagian yang disorot panduan. Saat langkahnya aktif, bagian ini digulir hingga terlihat.
 * [part] dipakai bila satu sorotan mencakup beberapa kartu (posisinya digabung).
 */
@OptIn(ExperimentalFoundationApi::class)
fun Modifier.tourTarget(target: TourTarget, part: Int = 0): Modifier = composed {
    val tour = LocalHomeTour.current
    if (tour == null) {
        Modifier
    } else {
        val requester = remember { BringIntoViewRequester() }
        val active = tour.activeTarget == target
        LaunchedEffect(active) {
            if (active && part == 0) requester.bringIntoView()
        }
        Modifier
            .bringIntoViewRequester(requester)
            .onGloballyPositioned { tour.bounds[TourKey(target, part)] = it.boundsInRoot() }
    }
}

/** Lapisan gelap dengan lubang di bagian yang dijelaskan + kartu panduan. Menahan semua sentuhan. */
@Composable
fun HomeTourOverlay(tour: HomeTourController) {
    val index = tour.stepIndex ?: return
    val step = steps[index]
    val density = LocalDensity.current
    var origin by remember { mutableStateOf(Offset.Zero) }
    var size by remember { mutableStateOf(IntSize.Zero) }
    val rect = tour.rectOf(step.target)?.translate(-origin)

    BackHandler { if (index > 0) tour.back() else tour.finish() }

    Box(
        Modifier
            .fillMaxSize()
            .onGloballyPositioned {
                origin = it.positionInRoot()
                size = it.size
            }
            .pointerInput(Unit) { detectTapGestures { } }
    ) {
        Canvas(
            Modifier
                .fillMaxSize()
                .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
        ) {
            drawRect(Color(0xA6301822))
            if (rect != null) {
                val pad = 8.dp.toPx()
                drawRoundRect(
                    color = Color.Black,
                    topLeft = Offset(rect.left - pad, rect.top - pad),
                    size = Size(rect.width + 2 * pad, rect.height + 2 * pad),
                    cornerRadius = CornerRadius(18.dp.toPx()),
                    blendMode = BlendMode.Clear
                )
            }
        }

        // Kartu di bawah, kecuali bagian yang disorot ada di separuh bawah layar.
        val h = size.height.toFloat()
        val card = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
        when {
            rect != null && h > 0 && rect.top > h * 0.8f -> {
                // Menu di bottom nav: kartu tepat di atasnya.
                val gap = with(density) { (h - rect.top).toDp() } + 16.dp
                TourCard(index, step, tour, card.align(Alignment.BottomCenter).padding(bottom = gap))
            }
            rect != null && h > 0 && rect.center.y > h * 0.5f ->
                TourCard(index, step, tour, card.align(Alignment.TopCenter).windowInsetsPadding(WindowInsets.statusBars).padding(top = 16.dp))
            else ->
                TourCard(index, step, tour, card.align(Alignment.BottomCenter).windowInsetsPadding(WindowInsets.navigationBars).padding(bottom = 16.dp))
        }
    }
}

@Composable
private fun TourCard(index: Int, step: TourStep, tour: HomeTourController, modifier: Modifier) {
    val last = index == steps.lastIndex
    Column(
        modifier
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White)
            .padding(20.dp)
            .semantics { liveRegion = LiveRegionMode.Polite }
    ) {
        Text(
            text = "${index + 1}/${steps.size}  •  PANDUAN ANEMOM",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = Pink500
        )
        Spacer(Modifier.height(8.dp))
        Text(step.title, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Maroon)
        Spacer(Modifier.height(8.dp))
        Text(step.body, style = MaterialTheme.typography.bodyLarge, color = MutedText)
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            steps.indices.forEach { i ->
                Box(
                    Modifier
                        .width(16.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(50))
                        .background(if (i <= index) Pink500 else Pink100)
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = tour::next,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Pink500, contentColor = Color.White),
            shape = RoundedCornerShape(50)
        ) {
            Text(if (last) "Selesai, Mulai Jelajah!" else "Lanjut", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
        }
        Spacer(Modifier.height(4.dp))
        Row(Modifier.fillMaxWidth()) {
            TextButton(onClick = tour::back, enabled = index > 0) {
                Text("Kembali", fontWeight = FontWeight.SemiBold, color = if (index > 0) Maroon else Color.Transparent)
            }
            Spacer(Modifier.weight(1f))
            if (!last) {
                TextButton(onClick = tour::finish) {
                    Text("Lewati", fontWeight = FontWeight.SemiBold, color = Maroon)
                }
            }
        }
    }
}

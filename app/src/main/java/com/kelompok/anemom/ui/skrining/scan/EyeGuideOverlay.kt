package com.kelompok.anemom.ui.skrining.scan

import android.graphics.RectF
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.kelompok.anemom.ui.theme.GreenText
import com.kelompok.anemom.ui.theme.MutedText
import kotlin.math.roundToInt

private val GuideWhite = Color.White
private val GuideAmber = Color(0xFFFFC107)
private val GuideGreen = Color(0xFF00C853)

/**
 * Bingkai kelopak bawah (sabit konjungtiva + lengkung mata di atasnya) di atas preview kamera + overlay segmentasi U-Net live + kotak pembatas
 * konjungtiva (potongan ketat yang akan dikirim) + kontur mata dari detektor wajah. Bingkai mata digambar di dalam kotak panduan ([GuideBox]); yang dipotong dan dikirim
 * tetap kotak panduan itu.
 *
 * Warna bingkai: putih = mencari mata, kuning = mata ditemukan / perlu tarik kelopak, hijau = siap.
 * Garis hijau tebal berjalan mengelilingi bingkai selama hitungan tahan ([LiveGuide.holdProgress]).
 *
 * [mirror] = kamera depan: preview tampil seperti cermin, frame analisis tidak, jadi overlay dibalik.
 */
@Composable
fun EyeGuideOverlay(
    guide: LiveGuide,
    frame: LiveFrame?,
    mirror: Boolean,
    showLive: Boolean,
    modifier: Modifier = Modifier
) {
    val mask = remember(frame) { frame?.maskOverlay?.asImageBitmap() }
    Canvas(modifier) {
        val r = GuideBox.rectInView(size.width, size.height)
        val shape = GuideShape.of(r)
        val eye = shape.window

        val dim = Path().apply {
            fillType = PathFillType.EvenOdd
            addRect(Rect(Offset.Zero, size))
            addPath(eye)
        }
        drawPath(dim, Color.Black.copy(alpha = 0.45f))

        if (showLive) {
            // Area tempat mask digambar: potongan ketat di sekitar konjungtiva bila ada, selain itu seluruh kotak.
            // Kamera depan: koordinat analisis belum dicermin, jadi kiri-kanan ditukar.
            val focus = frame?.focus
            val area = if (focus == null) {
                Rect(r.left, r.top, r.right, r.bottom)
            } else {
                val fl = if (mirror) 1f - focus[2] else focus[0]
                val fr = if (mirror) 1f - focus[0] else focus[2]
                Rect(
                    r.left + fl * r.width(),
                    r.top + focus[1] * r.height(),
                    r.left + fr * r.width(),
                    r.top + focus[3] * r.height()
                )
            }
            // Segmentasi live, hanya di dalam bingkai mata.
            if (mask != null) {
                clipPath(eye) {
                    scale(scaleX = if (mirror) -1f else 1f, scaleY = 1f, pivot = area.center) {
                        drawImage(
                            image = mask,
                            srcOffset = IntOffset.Zero,
                            srcSize = IntSize(mask.width, mask.height),
                            dstOffset = IntOffset(area.left.roundToInt(), area.top.roundToInt()),
                            dstSize = IntSize(area.width.roundToInt(), area.height.roundToInt())
                        )
                    }
                }
            }
            // Kotak pembatas konjungtiva (bagian yang akan dipotong dan dikirim).
            if (focus != null) {
                drawRoundRect(
                    color = if (guide.ready) GuideGreen else GuideAmber,
                    topLeft = area.topLeft,
                    size = area.size,
                    cornerRadius = CornerRadius(10.dp.toPx()),
                    style = Stroke(width = 2.dp.toPx())
                )
            }
            // Kontur mata dari detektor wajah (tahap jauh).
            frame?.eyeOutline?.let { pts ->
                for (i in 0 until pts.size / 2) {
                    val rx = pts[2 * i]
                    val x = r.left + (if (mirror) 1f - rx else rx) * r.width()
                    val y = r.top + pts[2 * i + 1] * r.height()
                    drawCircle(GuideAmber, radius = 2.5.dp.toPx(), center = Offset(x, y))
                }
            }
        }

        val base = when {
            !showLive -> GuideWhite
            guide.ready -> GuideGreen
            guide.checks.eyeInFrame -> GuideAmber
            else -> GuideWhite
        }
        // Garis tipis: tempat mata. Garis tebal: lengkung kelopak bawah (konjungtiva) yang dibidik.
        drawPath(shape.eyeArc, base.copy(alpha = 0.55f), style = Stroke(width = 1.5.dp.toPx()))
        drawPath(shape.lid, base, style = Stroke(width = 3.dp.toPx()))

        if (showLive && guide.holdProgress > 0f) {
            val measure = PathMeasure().apply { setPath(shape.lid, false) }
            val segment = Path()
            measure.getSegment(0f, measure.length * guide.holdProgress, segment, true)
            drawPath(segment, GuideGreen, style = Stroke(width = 7.dp.toPx(), cap = StrokeCap.Round))
        }
    }
}

/**
 * Bentuk bingkai di dalam kotak [r] (uji 28 Nov: mask lebih besar bila kamera dibidikkan langsung ke
 * kelopak bawah dari sedikit atas, bukan ke seluruh mata):
 *  - [lid]    : lengkung bulan sabit = kelopak bawah yang ditarik (konjungtiva), dibidik di tengah kotak.
 *  - [eyeArc] : lengkung atas tipis = tempat mata, di atas sabit.
 *  - [window] : gabungan keduanya; area di luar ini digelapkan dan mask live hanya digambar di dalamnya.
 * Yang dipotong dan dikirim tetap kotak panduan / potongan ketat, bukan bentuk ini.
 */
private class GuideShape(val lid: Path, val eyeArc: Path, val window: Path) {
    companion object {
        fun of(r: RectF): GuideShape {
            val w = r.width()
            val h = r.height()
            val cx = r.centerX()
            // Ujung kiri-kanan sabit (sudut mata).
            val endY = r.top + h * 0.18f
            val left = r.left + w * 0.04f
            val right = r.right - w * 0.04f
            // Tepi atas sabit = tepi kelopak (melengkung ke bawah, puncak ±0,35h);
            // tepi bawah = lipatan kelopak yang ditarik (±0,75h). Titik kontrol kuadrat = 2*puncak - ujung.
            val lidTopCtrl = r.top + h * (2 * 0.35f - 0.18f)
            val lidBottomCtrl = r.top + h * (2 * 0.75f - 0.18f)
            // Lengkung mata di atas: puncak ±-0,12h (sedikit di atas kotak).
            val eyeCtrl = r.top + h * (2 * -0.12f - 0.18f)

            val lid = Path().apply {
                moveTo(left, endY)
                quadraticTo(cx, lidTopCtrl, right, endY)
                quadraticTo(cx, lidBottomCtrl, left, endY)
                close()
            }
            val eyeArc = Path().apply {
                moveTo(left, endY)
                quadraticTo(cx, eyeCtrl, right, endY)
            }
            val window = Path().apply {
                moveTo(left, endY)
                quadraticTo(cx, eyeCtrl, right, endY)
                quadraticTo(cx, lidBottomCtrl, left, endY)
                close()
            }
            return GuideShape(lid, eyeArc, window)
        }
    }
}

/** Petunjuk besar di atas kamera, seperti aplikasi verifikasi wajah. */
@Composable
fun GuideHintBanner(text: String, ready: Boolean, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = Color.White,
        textAlign = TextAlign.Center,
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (ready) GuideGreen.copy(alpha = 0.85f) else Color.Black.copy(alpha = 0.6f))
            .padding(horizontal = 14.dp, vertical = 10.dp)
    )
}

/** Daftar periksa di bawah kamera. */
@Composable
fun GuideChecklist(checks: GuideChecks, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        CheckItem("Cahaya", checks.light)
        CheckItem("Posisi", checks.eyeInFrame)
        CheckItem("Kelopak", checks.lidPulled)
        CheckItem("Tajam", checks.sharpAndSteady)
    }
}

@Composable
private fun CheckItem(label: String, ok: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            if (ok) Icons.Outlined.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
            contentDescription = null,
            tint = if (ok) GreenText else MutedText,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (ok) GreenText else MutedText
        )
    }
}

/** Angka mentah untuk kalibrasi ambang ([LiveGuideConfig]); hanya tampil di build debug. */
fun LiveObservation.debugText(): String {
    fun pct(v: Float) = "${(v * 1000).roundToInt() / 10.0}%"
    val area = maskArea?.let { a -> seedMaskArea?.takeIf { it != a }?.let { "${pct(it)}→${pct(a)}" } ?: pct(a) } ?: "-"
    val eyeText = eye?.let { "mata ${(it.widthRatio * 100).roundToInt()}% yaw ${it.yaw.roundToInt()}" } ?: "wajah -"
    return "terang ${brightness.roundToInt()} · tajam ${sharpness.roundToInt()} · gerak ${"%.1f".format(motion)} · " +
        "mask $area · $eyeText"
}

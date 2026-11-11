package com.kelompok.anemom.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kelompok.anemom.data.model.HbPoint
import com.kelompok.anemom.data.model.HbRisk
import com.kelompok.anemom.ui.theme.AmberText
import com.kelompok.anemom.ui.theme.CardBorder
import com.kelompok.anemom.ui.theme.MutedText
import com.kelompok.anemom.ui.theme.Pink500
import com.kelompok.anemom.util.DateFormats
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

/**
 * Grafik garis tren Hb dengan garis batas anemia (11 g/dL) putus-putus.
 * Sumbu Y minimal 10-12 g/dL dan melebar otomatis jika ada nilai di luar rentang itu.
 * Jika titik lebih dari 6, label tanggal ditampilkan selang-seling agar tidak bertumpuk.
 */
@Composable
fun HbTrendChart(
    points: List<HbPoint>,
    modifier: Modifier = Modifier,
    height: Dp = 180.dp
) {
    val textMeasurer = rememberTextMeasurer()
    val axisStyle = TextStyle(fontSize = 10.sp, color = MutedText)
    val valueStyle = TextStyle(fontSize = 10.sp, color = Pink500, fontWeight = FontWeight.Bold)

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
    ) {
        if (points.isEmpty()) return@Canvas

        val minY = min(10f, floor(points.minOf { it.value }))
        val maxY = max(12f, ceil(points.maxOf { it.value }))
        val step = if (maxY - minY > 4f) 2 else 1

        val leftPad = 46.dp.toPx()
        val rightPad = 18.dp.toPx()
        val topPad = 22.dp.toPx()
        val bottomPad = 26.dp.toPx()
        val chartWidth = size.width - leftPad - rightPad
        val chartHeight = size.height - topPad - bottomPad

        fun xOf(index: Int): Float =
            if (points.size == 1) leftPad + chartWidth / 2f
            else leftPad + chartWidth * index / (points.size - 1)

        fun yOf(value: Float): Float =
            topPad + chartHeight * (1f - (value - minY) / (maxY - minY))

        // Label sumbu Y dan garis bantu tipis
        var tick = minY.toInt()
        while (tick <= maxY.toInt()) {
            val label = textMeasurer.measure("$tick g/dL", axisStyle)
            drawText(label, topLeft = Offset(0f, yOf(tick.toFloat()) - label.size.height / 2f))
            if (tick != HbRisk.REFERENCE_HB.toInt()) {
                drawLine(
                    color = CardBorder,
                    start = Offset(leftPad, yOf(tick.toFloat())),
                    end = Offset(size.width - rightPad, yOf(tick.toFloat())),
                    strokeWidth = 1.dp.toPx()
                )
            }
            tick += step
        }

        // Garis batas anemia (11 g/dL) putus-putus
        drawLine(
            color = AmberText.copy(alpha = 0.7f),
            start = Offset(leftPad, yOf(HbRisk.REFERENCE_HB)),
            end = Offset(size.width - rightPad, yOf(HbRisk.REFERENCE_HB)),
            strokeWidth = 1.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f))
        )

        // Garis tren
        if (points.size >= 2) {
            val path = Path()
            points.forEachIndexed { index, point ->
                val x = xOf(index)
                val y = yOf(point.value)
                if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            drawPath(
                path = path,
                color = Pink500,
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
        }

        // Titik, label nilai, dan label tanggal
        val labelEvery = if (points.size <= 6) 1 else ceil(points.size / 6f).toInt()
        points.forEachIndexed { index, point ->
            val center = Offset(xOf(index), yOf(point.value))
            drawCircle(color = Color.White, radius = 5.dp.toPx(), center = center)
            drawCircle(
                color = Pink500,
                radius = 5.dp.toPx(),
                center = center,
                style = Stroke(width = 2.dp.toPx())
            )

            val showLabel = index % labelEvery == 0 || index == points.lastIndex
            if (showLabel) {
                val valueLabel = textMeasurer.measure(DateFormats.hb(point.value), valueStyle)
                drawText(
                    valueLabel,
                    topLeft = Offset(center.x - valueLabel.size.width / 2f, center.y - 20.dp.toPx())
                )

                val dateLabel = textMeasurer.measure(point.label, axisStyle)
                drawText(
                    dateLabel,
                    topLeft = Offset(
                        center.x - dateLabel.size.width / 2f,
                        size.height - bottomPad + 8.dp.toPx()
                    )
                )
            }
        }
    }
}

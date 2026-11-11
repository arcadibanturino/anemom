package com.kelompok.anemom.ui.ttd

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Medication
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kelompok.anemom.ui.AppViewModelProvider
import com.kelompok.anemom.ui.components.AnemomCard
import com.kelompok.anemom.ui.components.AnemomChip
import com.kelompok.anemom.ui.components.AnemomScreen
import com.kelompok.anemom.ui.components.GradientButton
import com.kelompok.anemom.ui.theme.AmberBg
import com.kelompok.anemom.ui.theme.AmberText
import com.kelompok.anemom.ui.theme.CardBorder
import com.kelompok.anemom.ui.theme.GreenBg
import com.kelompok.anemom.ui.theme.GreenText
import com.kelompok.anemom.ui.theme.Maroon
import com.kelompok.anemom.ui.theme.MutedText
import com.kelompok.anemom.ui.theme.Pink100
import com.kelompok.anemom.ui.theme.Pink500
import com.kelompok.anemom.util.DateFormats
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

private val dayLabels = listOf("S", "S", "R", "K", "J", "S", "M") // Senin - Minggu
private val monthTitleFormat = DateTimeFormatter.ofPattern("MMMM yyyy", Locale("id", "ID"))

/**
 * Tab TTD: pelacak minum Tablet Tambah Darah.
 *
 * TODO(fitur): pengingat sungguhan (notifikasi terjadwal / FCM). Saat ini tombol "Pengingat aktif"
 * hanya menyimpan statusnya, dan jam pengingat tetap 20.00.
 */
@Composable
fun TtdScreen(
    onBack: () -> Unit,
    viewModel: TtdViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    AnemomScreen(
        title = "Pelacak TTD",
        subtitle = "Satu tablet, satu langkah lebih sehat",
        onBack = onBack
    ) {
        // Kartu TTD hari ini
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(Pink100)
                .padding(18.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Medication,
                        contentDescription = null,
                        tint = Pink500,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(
                        text = "TTD hari ini",
                        style = MaterialTheme.typography.titleLarge,
                        color = Maroon
                    )
                    Text(
                        text = "${DateFormats.short(state.today)} · Pukul 20.00",
                        style = MaterialTheme.typography.bodySmall,
                        color = MutedText
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (state.reminderOn) "Pengingat aktif · Setelah makan" else "Pengingat nonaktif",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = Maroon,
                    modifier = Modifier.weight(1f)
                )
                Switch(
                    checked = state.reminderOn,
                    onCheckedChange = viewModel::setReminder,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Pink500,
                        uncheckedThumbColor = Color.White,
                        uncheckedTrackColor = MutedText.copy(alpha = 0.4f),
                        uncheckedBorderColor = Color.Transparent
                    )
                )
            }
            Spacer(Modifier.height(8.dp))
            if (state.takenToday) {
                AnemomChip(text = "Sudah diminum", containerColor = GreenBg, contentColor = GreenText)
            } else {
                AnemomChip(text = "Belum diminum", containerColor = AmberBg, contentColor = AmberText)
            }
        }

        Spacer(Modifier.height(14.dp))
        GradientButton(
            text = if (state.takenToday) "TTD hari ini sudah ditandai" else "Tandai sudah minum TTD",
            onClick = viewModel::markTakenToday,
            enabled = !state.takenToday,
            icon = Icons.Filled.Check
        )

        Spacer(Modifier.height(14.dp))
        AnemomCard(Modifier.fillMaxWidth()) {
            CalendarHeader(
                month = state.month,
                onPrevious = viewModel::previousMonth,
                onNext = viewModel::nextMonth
            )
            Spacer(Modifier.height(8.dp))
            CalendarGrid(
                month = state.month,
                today = state.today,
                takenDates = state.takenDates
            )
            Spacer(Modifier.height(12.dp))
            CalendarLegend()
        }

        Spacer(Modifier.height(20.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Riwayat minum TTD",
                style = MaterialTheme.typography.titleMedium,
                color = Maroon,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "${state.takenLast7Days}/7 hari · ${state.percentLast7Days}%",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = Pink500
            )
        }
        Spacer(Modifier.height(8.dp))

        if (state.history.isEmpty()) {
            Text(
                text = "Belum ada catatan. Tekan \"Tandai sudah minum TTD\" setelah Bunda minum tablet.",
                style = MaterialTheme.typography.bodySmall,
                color = MutedText
            )
        } else {
            state.history.take(5).forEach { entry ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CheckCircle,
                        contentDescription = null,
                        tint = GreenText,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = "${DateFormats.short(entry.date)} · ${DateFormats.time(entry.time)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Maroon,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "Diminum",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = GreenText
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        Text(
            text = "Ikuti dosis dari tenaga kesehatan. Hindari teh atau kopi berdekatan dengan waktu minum TTD.",
            style = MaterialTheme.typography.bodySmall,
            color = MutedText
        )
    }
}

@Composable
private fun CalendarHeader(
    month: YearMonth,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPrevious) {
            Icon(Icons.Filled.ChevronLeft, contentDescription = "Bulan sebelumnya", tint = Pink500)
        }
        Text(
            text = month.format(monthTitleFormat),
            style = MaterialTheme.typography.titleMedium,
            color = Maroon,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = onNext) {
            Icon(Icons.Filled.ChevronRight, contentDescription = "Bulan berikutnya", tint = Pink500)
        }
    }
}

@Composable
private fun CalendarGrid(
    month: YearMonth,
    today: LocalDate,
    takenDates: Set<LocalDate>
) {
    // Minggu dimulai dari Senin: Senin = 0 ... Minggu = 6
    val leadingBlanks = month.atDay(1).dayOfWeek.value - 1
    val cells: List<Int?> = List(leadingBlanks) { null } + (1..month.lengthOfMonth()).toList()
    val rows = cells.chunked(7)

    Row(modifier = Modifier.fillMaxWidth()) {
        dayLabels.forEach { label ->
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MutedText,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )
        }
    }
    Spacer(Modifier.height(6.dp))

    rows.forEach { week ->
        Row(modifier = Modifier.fillMaxWidth()) {
            repeat(7) { column ->
                val day = week.getOrNull(column)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (day != null) {
                        val date = month.atDay(day)
                        DayCell(
                            day = day,
                            isToday = date == today,
                            isTaken = date in takenDates
                        )
                    } else {
                        Spacer(Modifier.size(36.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(day: Int, isToday: Boolean, isTaken: Boolean) {
    val background = when {
        isToday -> Pink500
        isTaken -> Pink100
        else -> Color.Transparent
    }
    val textColor = when {
        isToday -> Color.White
        isTaken -> Pink500
        else -> Maroon
    }
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(background),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = day.toString(),
            fontSize = 13.sp,
            fontWeight = if (isToday || isTaken) FontWeight.Bold else FontWeight.Normal,
            color = textColor
        )
    }
}

@Composable
private fun CalendarLegend() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LegendItem(label = "Diminum", color = Pink100, borderColor = Pink500.copy(alpha = 0.4f))
        LegendItem(label = "Hari ini", color = Pink500, borderColor = Pink500)
        LegendItem(label = "Belum dijadwalkan", color = Color.Transparent, borderColor = CardBorder)
    }
}

@Composable
private fun LegendItem(label: String, color: Color, borderColor: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
                .border(1.dp, borderColor, CircleShape)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = label,
            fontSize = 10.sp,
            color = MutedText
        )
    }
}

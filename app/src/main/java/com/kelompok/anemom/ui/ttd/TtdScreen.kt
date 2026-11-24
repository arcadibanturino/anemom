package com.kelompok.anemom.ui.ttd

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.outlined.Medication
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import com.kelompok.anemom.alarm.AlarmTimeText
import com.kelompok.anemom.data.repository.TtdDay
import com.kelompok.anemom.data.repository.TtdStatus
import com.kelompok.anemom.data.repository.TtdTrackerData
import com.kelompok.anemom.data.repository.TtdWeek
import com.kelompok.anemom.ui.AppViewModelProvider
import com.kelompok.anemom.ui.components.AnemomCard
import com.kelompok.anemom.ui.components.AnemomChip
import com.kelompok.anemom.ui.components.AnemomTabHeader
import com.kelompok.anemom.ui.components.GradientButton
import com.kelompok.anemom.ui.theme.AmberBg
import com.kelompok.anemom.ui.theme.AmberText
import com.kelompok.anemom.ui.theme.CardBorder
import com.kelompok.anemom.ui.theme.ErrorRed
import com.kelompok.anemom.ui.theme.GreenBg
import com.kelompok.anemom.ui.theme.GreenText
import com.kelompok.anemom.ui.theme.Maroon
import com.kelompok.anemom.ui.theme.MutedText
import com.kelompok.anemom.ui.theme.Pink100
import com.kelompok.anemom.ui.theme.Pink500
import com.kelompok.anemom.ui.theme.RedBg
import com.kelompok.anemom.util.DateFormats
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

private val indonesian = Locale.forLanguageTag("id-ID")
private val dayLabels = listOf("S", "S", "R", "K", "J", "S", "M") // Senin - Minggu
private val monthTitleFormat = DateTimeFormatter.ofPattern("MMMM yyyy", indonesian)
private val dayMonth = DateTimeFormatter.ofPattern("d MMM", indonesian)
private val dayOnly = DateTimeFormatter.ofPattern("d", indonesian)
private val BarLight = Color(0xFFF4A6C0)

/** Tab TTD: Pelacak TTD. Alarm harian diatur di Pengaturan pengingat ("Edit" membuka Ubah jam alarm). */
@Suppress("UNUSED_PARAMETER")
@Composable
fun TtdScreen(
    onBack: () -> Unit,
    onEditAlarm: () -> Unit = {},
    viewModel: TtdViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        AnemomTabHeader(title = "Pelacak TTD", subtitle = "Satu tablet, satu langkah lebih sehat")
        Spacer(Modifier.height(16.dp))

        val data = state.data
        if (data == null) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 48.dp),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator(color = Pink500) }
        } else {
            TodayCard(data, onEditAlarm)
            Spacer(Modifier.height(14.dp))
            val today = data.today
            GradientButton(
                text = if (today.status == TtdStatus.SUDAH) {
                    "Sudah dicatat · ${AlarmTimeText.timeWithZone(today.time)}"
                } else {
                    "Tandai sudah minum TTD"
                },
                onClick = viewModel::markTakenToday,
                modifier = Modifier.fillMaxWidth(),
                enabled = today.status != TtdStatus.SUDAH && !state.isMarking,
                isLoading = state.isMarking,
                icon = Icons.Filled.Check
            )

            Spacer(Modifier.height(14.dp))
            AnemomCard(Modifier.fillMaxWidth()) {
                CalendarHeader(data.month, viewModel::previousMonth, viewModel::nextMonth)
                Spacer(Modifier.height(8.dp))
                CalendarGrid(data.month, LocalDate.now(), data.takenDates, data.skippedDates)
                Spacer(Modifier.height(12.dp))
                CalendarLegend()
            }

            Spacer(Modifier.height(14.dp))
            WeeklyChart(data)

            Spacer(Modifier.height(20.dp))
            History(data)

            Spacer(Modifier.height(12.dp))
            Text(
                text = "Ikuti dosis dari tenaga kesehatan. Hindari teh atau kopi berdekatan dengan waktu minum TTD.",
                style = MaterialTheme.typography.bodySmall,
                color = MutedText
            )
            Spacer(Modifier.height(12.dp))
            AnemomCard(Modifier.fillMaxWidth(), containerColor = Pink100.copy(alpha = 0.75f)) {
                Text(
                    text = "Sudah: dikonfirmasi diminum. Belum: dilaporkan belum diminum. " +
                        "Belum Dikonfirmasi: belum ada jawaban.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Maroon
                )
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun TodayCard(data: TtdTrackerData, onEditAlarm: () -> Unit) {
    val today = data.today
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Pink100)
            .border(1.dp, CardBorder, RoundedCornerShape(24.dp))
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
                Icon(Icons.Outlined.Medication, contentDescription = null, tint = Pink500, modifier = Modifier.size(28.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text("TTD hari ini", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Maroon)
                Text(
                    text = "${DateFormats.short(today.date)} · Pukul ${AlarmTimeText.timeWithZone(data.reminderTime)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MutedText
                )
            }
            AnemomChip(
                text = "Edit",
                containerColor = AmberBg,
                contentColor = Maroon,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .clickable(onClickLabel = "Ubah jam alarm", onClick = onEditAlarm)
            )
        }
        Spacer(Modifier.height(10.dp))
        Text(if (data.reminderOn) "Alarm aktif" else "Alarm nonaktif", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = Maroon)
        Spacer(Modifier.height(8.dp))
        StatusChip(today.status)
    }
}

@Composable
private fun StatusChip(status: TtdStatus) {
    when (status) {
        TtdStatus.SUDAH -> AnemomChip(status.label, containerColor = GreenBg, contentColor = GreenText)
        TtdStatus.BELUM -> AnemomChip(status.label, containerColor = RedBg, contentColor = ErrorRed)
        TtdStatus.BELUM_DIKONFIRMASI -> AnemomChip(status.label, containerColor = AmberBg, contentColor = AmberText)
    }
}

private fun statusColor(status: TtdStatus): Color = when (status) {
    TtdStatus.SUDAH -> GreenText
    TtdStatus.BELUM -> ErrorRed
    TtdStatus.BELUM_DIKONFIRMASI -> Pink500
}

@Composable
private fun CalendarHeader(month: YearMonth, onPrevious: () -> Unit, onNext: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onPrevious) {
            Icon(Icons.Filled.ChevronLeft, contentDescription = "Bulan sebelumnya", tint = Pink500)
        }
        Text(
            text = month.format(monthTitleFormat),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
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
    takenDates: Set<LocalDate>,
    skippedDates: Set<LocalDate>
) {
    // Minggu dimulai dari Senin: Senin = 0 ... Minggu = 6
    val leadingBlanks = month.atDay(1).dayOfWeek.value - 1
    val cells: List<Int?> = List(leadingBlanks) { null } + (1..month.lengthOfMonth()).toList()

    Row(Modifier.fillMaxWidth()) {
        dayLabels.forEach {
            Text(it, style = MaterialTheme.typography.labelMedium, color = MutedText, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
        }
    }
    Spacer(Modifier.height(6.dp))
    cells.chunked(7).forEach { week ->
        Row(Modifier.fillMaxWidth()) {
            repeat(7) { column ->
                val day = week.getOrNull(column)
                Box(
                    Modifier
                        .weight(1f)
                        .padding(2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (day != null) {
                        val date = month.atDay(day)
                        DayCell(day, isToday = date == today, isTaken = date in takenDates, isSkipped = date in skippedDates)
                    } else {
                        Spacer(Modifier.size(36.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(day: Int, isToday: Boolean, isTaken: Boolean, isSkipped: Boolean) {
    val background = when {
        isToday && isTaken -> Pink500
        isTaken -> Pink100
        else -> Color.Transparent
    }
    val border = when {
        isToday && !isTaken -> Pink500
        isSkipped -> ErrorRed.copy(alpha = 0.5f)
        else -> Color.Transparent
    }
    val textColor = when {
        isToday && isTaken -> Color.White
        isTaken || isToday -> Pink500
        else -> Maroon
    }
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(background)
            .border(1.5.dp, border, CircleShape),
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
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LegendItem("Diminum", Pink100, Pink500)
        LegendItem("Hari ini", Color.Transparent, Pink500)
        LegendItem("Belum", Color.Transparent, ErrorRed.copy(alpha = 0.5f))
        LegendItem("Belum dijadwalkan", Color.Transparent, CardBorder)
    }
}

@Composable
private fun LegendItem(label: String, color: Color, borderColor: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(9.dp)
                .clip(CircleShape)
                .background(color)
                .border(1.dp, borderColor, CircleShape)
        )
        Spacer(Modifier.width(5.dp))
        Text(label, fontSize = 10.sp, color = MutedText)
    }
}

/** Kepatuhan 4 minggu terakhir; batang terakhir = 7 hari terakhir. */
@Composable
private fun WeeklyChart(data: TtdTrackerData) {
    val last = data.weeks.last()
    AnemomCard(Modifier.fillMaxWidth()) {
        Text("Kepatuhan mingguan (%)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Maroon)
        Spacer(Modifier.height(8.dp))
        Text(
            text = "${data.summary.takenLast7Days} dari 7 jadwal terpenuhi · ${data.summary.percentLast7Days}%",
            style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp),
            fontWeight = FontWeight.Bold,
            color = Pink500
        )
        Text(
            text = "${rangeText(last)} · 7 hari terakhir",
            style = MaterialTheme.typography.bodySmall,
            color = MutedText
        )
        Spacer(Modifier.height(16.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Bottom
        ) {
            data.weeks.forEachIndexed { index, week ->
                val isLast = index == data.weeks.lastIndex
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom
                ) {
                    Text("${week.percent}%", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Maroon)
                    Spacer(Modifier.height(6.dp))
                    Box(
                        Modifier
                            .width(36.dp)
                            .height((100 * week.percent / 100f).coerceAtLeast(4f).dp)
                            .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp, bottomStart = 2.dp, bottomEnd = 2.dp))
                            .background(if (isLast) Pink500 else BarLight)
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "${week.start.format(dayOnly)}–${week.end.format(dayMonth)}",
                        fontSize = 9.sp,
                        color = MutedText,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Text("0–100% · Jumlah Sudah ÷ jadwal × 100", style = MaterialTheme.typography.labelSmall, color = MutedText)
    }
}

private fun rangeText(week: TtdWeek): String =
    "${week.start.format(dayMonth)}–${DateFormats.short(week.end)}"

@Composable
private fun History(data: TtdTrackerData) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "Riwayat minum TTD",
            style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp),
            fontWeight = FontWeight.Bold,
            color = Maroon,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = "${data.summary.takenLast7Days}/7 · ${data.summary.percentLast7Days}%",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = Pink500
        )
    }
    Spacer(Modifier.height(6.dp))
    data.last7Days.forEach { day -> HistoryRow(day) }
}

@Composable
private fun HistoryRow(day: TtdDay) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "${day.date.format(dayMonth)} · ${AlarmTimeText.time(day.time)}",
            style = MaterialTheme.typography.bodyMedium,
            color = Maroon,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = day.status.label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = statusColor(day.status)
        )
    }
}

package com.kelompok.anemom.ui.notifikasi

import android.widget.Toast
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Medication
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kelompok.anemom.data.model.AppNotification
import com.kelompok.anemom.data.model.NotificationType
import com.kelompok.anemom.ui.AppViewModelProvider
import com.kelompok.anemom.ui.components.AnemomChip
import com.kelompok.anemom.ui.components.AnemomOutlinedButton
import com.kelompok.anemom.ui.components.AnemomScreen
import com.kelompok.anemom.ui.theme.CardBorder
import com.kelompok.anemom.ui.theme.Maroon
import com.kelompok.anemom.ui.theme.MutedText
import com.kelompok.anemom.ui.theme.Pink100
import com.kelompok.anemom.ui.theme.Pink500
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val indonesian = Locale.forLanguageTag("id-ID")
private val dayHeaderFormat = DateTimeFormatter.ofPattern("d MMMM", indonesian)
private val itemTimeFormat = DateTimeFormatter.ofPattern("d MMM yyyy · HH.mm", indonesian)

/** Notifikasi, dibuka dari ikon hati di Beranda. */
@Composable
fun NotificationsScreen(
    onBack: () -> Unit,
    onOpenTtd: () -> Unit,
    onOpenInputHb: () -> Unit,
    viewModel: NotificationsViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(state.message) {
        state.message?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.messageShown()
        }
    }

    AnemomScreen(
        title = "Notifikasi",
        subtitle = "Pengingat kecil untuk Bunda",
        onBack = onBack
    ) {
        when {
            state.isLoading && state.items.isEmpty() -> Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 48.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Pink500)
            }

            state.error != null && state.items.isEmpty() -> {
                Spacer(Modifier.height(16.dp))
                Text(state.error.orEmpty(), style = MaterialTheme.typography.bodyMedium, color = MutedText)
                Spacer(Modifier.height(12.dp))
                AnemomOutlinedButton(text = "Coba lagi", onClick = viewModel::load, modifier = Modifier.fillMaxWidth())
            }

            state.items.isEmpty() -> {
                Spacer(Modifier.height(24.dp))
                Text(
                    text = "Belum ada notifikasi. Pengingat TTD dan periksa Hb akan muncul di sini.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MutedText
                )
            }

            else -> NotificationList(
                state = state,
                onMarkAll = viewModel::markAllRead,
                onClick = { item ->
                    viewModel.markRead(item)
                    when (item.type) {
                        NotificationType.TTD_REMINDER -> onOpenTtd()
                        NotificationType.HB_REMINDER -> onOpenInputHb()
                        else -> Unit
                    }
                }
            )
        }
    }
}

@Composable
private fun NotificationList(
    state: NotificationsUiState,
    onMarkAll: () -> Unit,
    onClick: (AppNotification) -> Unit
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        if (state.unread > 0) {
            AnemomChip("${state.unread} belum dibaca", containerColor = Pink100, contentColor = Pink500)
        } else {
            AnemomChip("Semua sudah dibaca", containerColor = Pink100, contentColor = MutedText)
        }
        Spacer(Modifier.weight(1f))
        if (state.unread > 0) {
            Text(
                text = if (state.isMarkingAll) "Menandai..." else "Tandai semua dibaca",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = Pink500,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(enabled = !state.isMarkingAll, onClick = onMarkAll)
                    .padding(horizontal = 4.dp, vertical = 6.dp)
            )
        }
    }

    val zone = ZoneId.systemDefault()
    val today = LocalDate.now(zone)
    val (todayItems, olderItems) = state.items.partition { it.time?.atZone(zone)?.toLocalDate() == today }

    if (todayItems.isNotEmpty()) {
        SectionTitle("Hari ini · ${today.format(dayHeaderFormat)}")
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            todayItems.forEach { NotificationCard(it, zone, onClick) }
        }
    }
    if (olderItems.isNotEmpty()) {
        SectionTitle("Sebelumnya")
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            olderItems.forEach { NotificationCard(it, zone, onClick) }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Spacer(Modifier.height(20.dp))
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp),
        fontWeight = FontWeight.Bold,
        color = Maroon
    )
    Spacer(Modifier.height(10.dp))
}

@Composable
private fun NotificationCard(item: AppNotification, zone: ZoneId, onClick: (AppNotification) -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    val unread = !item.isRead
    val action = actionLabel(item.type)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (unread) Pink100.copy(alpha = 0.75f) else Color.White)
            .border(1.dp, if (unread) Pink500.copy(alpha = 0.25f) else CardBorder, shape)
            .clickable { onClick(item) }
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (unread) Color.White else Pink100),
                contentAlignment = Alignment.Center
            ) {
                Icon(iconFor(item.type), contentDescription = null, tint = Pink500, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(12.dp))
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = Maroon,
                modifier = Modifier.weight(1f)
            )
            if (unread) {
                Spacer(Modifier.width(8.dp))
                Box(
                    Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Pink500)
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(item.body, style = MaterialTheme.typography.bodySmall, color = MutedText)
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = item.time?.atZone(zone)?.format(itemTimeFormat).orEmpty(),
                style = MaterialTheme.typography.labelSmall,
                color = MutedText,
                modifier = Modifier.weight(1f)
            )
            if (action != null) {
                Text(
                    text = action,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = Pink500
                )
            }
        }
    }
}

private fun iconFor(type: NotificationType): ImageVector = when (type) {
    NotificationType.TTD_REMINDER -> Icons.Outlined.Medication
    NotificationType.TTD_DONE -> Icons.Outlined.CheckCircle
    NotificationType.HB_REMINDER -> Icons.Outlined.WaterDrop
    NotificationType.CHECKUP -> Icons.Outlined.CalendarMonth
    NotificationType.INFO -> Icons.Outlined.Info
}

/** Teks aksi di kanan bawah kartu; null = tidak ada layar tujuan. */
private fun actionLabel(type: NotificationType): String? = when (type) {
    NotificationType.TTD_REMINDER -> "Lihat pelacak TTD"
    NotificationType.HB_REMINDER -> "Catat hasil Hb"
    else -> null
}

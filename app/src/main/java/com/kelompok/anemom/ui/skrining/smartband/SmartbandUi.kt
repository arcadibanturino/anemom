package com.kelompok.anemom.ui.skrining.smartband

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Watch
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.kelompok.anemom.data.smartband.SmartbandInfo
import com.kelompok.anemom.ui.theme.Pink100
import com.kelompok.anemom.ui.theme.Pink500
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Komponen kecil yang dipakai bersama layar Koneksi Bluetooth dan Rekam PPG. */

private val syncTimeFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("HH.mm")

/** "Baterai 82% · Sinkron terakhir 08.42" */
internal fun SmartbandInfo.detailLine(): String = buildList {
    batteryPercent?.let { add("Baterai $it%") }
    lastSyncAtMillis?.let {
        val time = Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).format(syncTimeFormat)
        add("Sinkron terakhir $time")
    }
}.joinToString(" · ")

@Composable
internal fun DeviceIconBox(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(52.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Pink100),
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Outlined.Watch, contentDescription = null, tint = Pink500, modifier = Modifier.size(26.dp))
    }
}

@Composable
internal fun StatusDot(text: String, dotColor: Color, textColor: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(dotColor)
        )
        Spacer(Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.bodySmall, color = textColor)
    }
}

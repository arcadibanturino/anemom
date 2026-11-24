package com.kelompok.anemom.ui.skrining.smartband

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MonitorHeart
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kelompok.anemom.data.smartband.SmartbandInfo
import com.kelompok.anemom.ui.AppViewModelProvider
import com.kelompok.anemom.ui.components.AnemomCard
import com.kelompok.anemom.ui.components.AnemomChip
import com.kelompok.anemom.ui.components.AnemomOutlinedButton
import com.kelompok.anemom.ui.components.AnemomScreen
import com.kelompok.anemom.ui.components.GradientButton
import com.kelompok.anemom.ui.components.NoteCard
import com.kelompok.anemom.ui.theme.CardBorder
import com.kelompok.anemom.ui.theme.GreenText
import com.kelompok.anemom.ui.theme.Maroon
import com.kelompok.anemom.ui.theme.MutedText
import com.kelompok.anemom.ui.theme.Pink100
import com.kelompok.anemom.ui.theme.Pink500

/**
 * Koneksi Bluetooth: layar pertama saat "Smartband / PPG" di tab Skrining ditekan.
 * Datanya masih SIMULASI sampai perangkat IoT siap (lihat SmartbandRepository).
 */
@Composable
fun SmartbandConnectScreen(
    onBack: () -> Unit,
    onRecordPpg: () -> Unit,
    viewModel: SmartbandConnectViewModel = viewModel(factory = AppViewModelProvider.Factory)
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
        title = "Koneksi Bluetooth",
        subtitle = "Kelola smartband Bunda",
        onBack = onBack
    ) {
        if (state.isSimulated) {
            AnemomChip("Data simulasi", containerColor = Pink100, contentColor = Pink500)
            Spacer(Modifier.height(12.dp))
        }

        BluetoothCard(
            enabled = state.bluetoothEnabled,
            isSimulated = state.isSimulated,
            onToggle = viewModel::setBluetoothEnabled
        )

        SectionTitle("Perangkat Bunda")
        val connected = state.connected
        if (connected != null) {
            ConnectedDeviceCard(
                device = connected,
                isDisconnecting = state.isDisconnecting,
                onRecordPpg = onRecordPpg,
                onDisconnect = viewModel::disconnect
            )
        } else {
            InfoCard(
                if (state.bluetoothEnabled) "Belum ada smartband yang tersambung. Pilih perangkat di bawah."
                else "Nyalakan Bluetooth untuk menyambungkan smartband."
            )
        }

        SectionTitle("Perangkat tersedia")
        when {
            !state.bluetoothEnabled -> InfoCard("Bluetooth mati.")
            state.isScanning && state.available.isEmpty() -> ScanningCard()
            state.available.isEmpty() -> InfoCard(
                "Tidak ada smartband lain di sekitar. Pastikan smartband menyala, lalu cari ulang."
            )
            else -> state.available.forEachIndexed { index, device ->
                if (index > 0) Spacer(Modifier.height(12.dp))
                AvailableDeviceCard(
                    device = device,
                    isConnecting = state.connectingId == device.id,
                    enabled = state.connectingId == null,
                    onConnect = { viewModel.connect(device) }
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        AnemomOutlinedButton(
            text = if (state.isScanning) "Mencari perangkat..." else "Cari ulang perangkat",
            onClick = viewModel::scan,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(16.dp))
        NoteCard(
            "Dekatkan smartband ke ponsel dan pastikan smartband menyala. " +
                "Pilih hanya perangkat milik Bunda sebelum menghubungkannya."
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = "Koneksi perangkat mendukung perekaman sinyal PPG untuk skrining awal, " +
                "bukan pengukuran Hb darah atau diagnosis medis.",
            style = MaterialTheme.typography.bodySmall,
            color = MutedText
        )
    }
}

@Composable
private fun BluetoothCard(enabled: Boolean, isSimulated: Boolean, onToggle: (Boolean) -> Unit) {
    AnemomCard(Modifier.fillMaxWidth(), containerColor = Pink100.copy(alpha = 0.6f)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = if (enabled) "Bluetooth aktif" else "Bluetooth mati",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Maroon
                )
                Text(
                    text = if (enabled) "Ponsel siap terhubung ke smartband" else "Nyalakan untuk mencari smartband",
                    style = MaterialTheme.typography.bodySmall,
                    color = MutedText
                )
            }
            Switch(
                checked = enabled,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = Pink500,
                    checkedBorderColor = Pink500,
                    uncheckedThumbColor = MutedText,
                    uncheckedTrackColor = Color.White,
                    uncheckedBorderColor = CardBorder
                )
            )
        }
        Spacer(Modifier.height(8.dp))
        // TODO(ble): tampilkan status izin BLUETOOTH_SCAN/CONNECT yang sebenarnya.
        Text(
            text = if (isSimulated) "Izin perangkat sekitar belum diminta · Data simulasi"
            else "Izin perangkat sekitar diberikan",
            style = MaterialTheme.typography.bodySmall,
            color = MutedText
        )
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
private fun DeviceHeader(device: SmartbandInfo, detail: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        DeviceIconBox()
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = device.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Maroon
            )
            if (detail.isNotEmpty()) {
                Text(detail, style = MaterialTheme.typography.bodySmall, color = MutedText)
            }
        }
    }
}

@Composable
private fun ConnectedDeviceCard(
    device: SmartbandInfo,
    isDisconnecting: Boolean,
    onRecordPpg: () -> Unit,
    onDisconnect: () -> Unit
) {
    AnemomCard(Modifier.fillMaxWidth()) {
        DeviceHeader(device, device.detailLine())
        Spacer(Modifier.height(12.dp))
        StatusDot("Tersambung · ${device.signal.label}", dotColor = GreenText, textColor = GreenText)
        Spacer(Modifier.height(14.dp))
        // Aksi utama di atas, aksi memutus di bawah supaya tidak tertekan tanpa sengaja.
        GradientButton(
            text = "Rekam sinyal PPG",
            onClick = onRecordPpg,
            modifier = Modifier.fillMaxWidth(),
            enabled = !isDisconnecting,
            icon = Icons.Outlined.MonitorHeart
        )
        Spacer(Modifier.height(10.dp))
        AnemomOutlinedButton(
            text = if (isDisconnecting) "Memutuskan..." else "Putuskan",
            onClick = onDisconnect,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun AvailableDeviceCard(
    device: SmartbandInfo,
    isConnecting: Boolean,
    enabled: Boolean,
    onConnect: () -> Unit
) {
    AnemomCard(Modifier.fillMaxWidth()) {
        DeviceHeader(device, "Perangkat di sekitar · Sensor PPG")
        Spacer(Modifier.height(12.dp))
        StatusDot(
            text = "Tersedia · Belum tersambung",
            dotColor = Pink500.copy(alpha = 0.45f),
            textColor = MutedText
        )
        Spacer(Modifier.height(14.dp))
        GradientButton(
            text = "Hubungkan",
            onClick = onConnect,
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled,
            isLoading = isConnecting
        )
    }
}

@Composable
private fun ScanningCard() {
    AnemomCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = Pink500,
                strokeWidth = 2.dp
            )
            Spacer(Modifier.width(12.dp))
            Text("Mencari smartband di sekitar...", style = MaterialTheme.typography.bodyMedium, color = MutedText)
        }
    }
}

@Composable
private fun InfoCard(text: String) {
    AnemomCard(Modifier.fillMaxWidth()) {
        Text(text, style = MaterialTheme.typography.bodyMedium, color = MutedText)
    }
}

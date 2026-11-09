package com.kelompok.anemom.ui.profile

import android.widget.Toast
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.outlined.GppGood
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kelompok.anemom.ui.AppViewModelProvider
import com.kelompok.anemom.ui.components.AnemomTopBar
import com.kelompok.anemom.ui.components.MomAvatar
import com.kelompok.anemom.ui.theme.CardBorder
import com.kelompok.anemom.ui.theme.ErrorRed
import com.kelompok.anemom.ui.theme.Maroon
import com.kelompok.anemom.ui.theme.MutedText
import com.kelompok.anemom.ui.theme.Pink100
import com.kelompok.anemom.ui.theme.Pink500
import com.kelompok.anemom.util.DateFormats
import com.kelompok.anemom.util.PregnancyCalculator
import java.time.LocalDate

private const val NOT_FILLED = "Belum diisi"

/**
 * Halaman "Profil saya" sesuai desain Figma.
 *
 * TODO(backend/fitur): tanggal lahir, fasilitas kesehatan, status kehamilan pertama, dan
 * tombol Edit belum punya datanya (belum ada di form Register); sementara tampil "Belum diisi".
 */
@Composable
fun ProfileScreen(
    onBack: () -> Unit,
    onOpenAbout: () -> Unit,
    onOpenFeature: (featureName: String) -> Unit,
    onLoggedOut: () -> Unit,
    viewModel: ProfileViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val account by viewModel.account.collectAsStateWithLifecycle()
    var showLogoutDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val hpht = remember(account) {
        account?.hpht?.takeIf { it.isNotBlank() }
            ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
    }
    val weeks = hpht?.let { PregnancyCalculator.weeksSince(it) }
    val dueDate = hpht?.let { DateFormats.long(PregnancyCalculator.dueDate(it)) }

    val fullName = account?.name.orEmpty()
    val firstName = fullName.trim().split(" ").firstOrNull().orEmpty()
    val comingSoon = {
        Toast.makeText(context, "Fitur edit segera hadir", Toast.LENGTH_SHORT).show()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        AnemomTopBar(
            title = "Profil saya",
            subtitle = "Kenali Bunda lebih dekat",
            onBack = onBack
        )

        // Header profil
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MomAvatar(size = 76.dp)
            Spacer(Modifier.width(16.dp))
            Column {
                Text(
                    text = if (firstName.isBlank()) "Bunda" else "Bunda $firstName",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Maroon
                )
                Text(
                    text = fullName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MutedText
                )
            }
        }

        SectionHeader(title = "Data pribadi", onEdit = comingSoon)
        InfoCard(
            rows = listOf(
                "Tanggal lahir" to NOT_FILLED,
                "Email" to (account?.email ?: "-"),
                "Nomor telepon" to (account?.phone ?: "-")
            ),
            containerColor = Color.White
        )

        SectionHeader(title = "Data kehamilan", onEdit = comingSoon)
        InfoCard(
            rows = listOf(
                "Usia kehamilan" to (weeks?.let { "$it minggu" } ?: "-"),
                "Trimester" to (account?.let { trimesterShort(it.trimester) } ?: "-"),
                "Perkiraan lahir" to (dueDate ?: "-"),
                "Fasilitas kesehatan" to NOT_FILLED
            ),
            containerColor = Pink100.copy(alpha = 0.7f)
        )

        Spacer(Modifier.height(8.dp))
        MenuItem(
            icon = Icons.Outlined.NotificationsNone,
            label = "Pengaturan pengingat",
            onClick = { onOpenFeature("Pengaturan pengingat") }
        )
        MenuItem(
            icon = Icons.Outlined.GppGood,
            label = "Privasi & disclaimer",
            onClick = { onOpenFeature("Privasi & disclaimer") }
        )
        MenuItem(
            icon = Icons.Outlined.Info,
            label = "Tentang Anemom",
            onClick = onOpenAbout
        )

        Spacer(Modifier.height(16.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clip(RoundedCornerShape(28.dp))
                .border(1.dp, Pink500, RoundedCornerShape(28.dp))
                .clickable { showLogoutDialog = true },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Keluar akun",
                color = Pink500,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
        Spacer(Modifier.height(24.dp))
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            containerColor = Color.White,
            title = { Text("Keluar dari Anemom?") },
            text = { Text("Bunda perlu masuk kembali untuk memakai aplikasi.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                        viewModel.logout(onDone = onLoggedOut)
                    }
                ) { Text("Keluar", color = ErrorRed) }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) { Text("Batal") }
            }
        )
    }
}

private fun trimesterShort(trimester: Int): String = when (trimester) {
    1 -> "Pertama"
    2 -> "Kedua"
    else -> "Ketiga"
}

@Composable
private fun SectionHeader(title: String, onEdit: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = Maroon,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = "Edit",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = Pink500,
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClick = onEdit)
                .padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
private fun InfoCard(rows: List<Pair<String, String>>, containerColor: Color) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(containerColor)
            .border(1.dp, CardBorder, RoundedCornerShape(18.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        rows.forEach { (label, value) ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall,
                    color = MutedText,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = if (value == NOT_FILLED) MutedText else Maroon,
                    textAlign = TextAlign.End
                )
            }
        }
    }
}

@Composable
private fun MenuItem(icon: ImageVector, label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = Pink500)
        Spacer(Modifier.width(14.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = Maroon,
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = Icons.Filled.ChevronRight,
            contentDescription = null,
            tint = Pink500
        )
    }
}

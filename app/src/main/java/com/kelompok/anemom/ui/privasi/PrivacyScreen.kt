package com.kelompok.anemom.ui.privasi

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.outlined.GppGood
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.RemoveRedEye
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kelompok.anemom.data.repository.ConsentRepository
import com.kelompok.anemom.ui.AppViewModelProvider
import com.kelompok.anemom.ui.components.AnemomCard
import com.kelompok.anemom.ui.components.AnemomScreen
import com.kelompok.anemom.ui.components.GradientButton
import com.kelompok.anemom.ui.theme.AmberBg
import com.kelompok.anemom.ui.theme.AmberText
import com.kelompok.anemom.ui.theme.Maroon
import com.kelompok.anemom.ui.theme.MutedText
import com.kelompok.anemom.ui.theme.Pink100
import com.kelompok.anemom.ui.theme.Pink500
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

data class PrivacyUiState(
    val acceptedAt: Instant? = null,
    val checked: Boolean = false,
    val isSaving: Boolean = false,
    val done: Boolean = false,
    val message: String? = null
)

class PrivacyViewModel(private val repository: ConsentRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(PrivacyUiState())
    val uiState: StateFlow<PrivacyUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.acceptedAt().onSuccess { at -> _uiState.update { it.copy(acceptedAt = at, checked = at != null) } }
        }
    }

    fun setChecked(value: Boolean) = _uiState.update { it.copy(checked = value) }

    fun accept() {
        val s = _uiState.value
        if (!s.checked || s.isSaving) return
        if (s.acceptedAt != null) {
            _uiState.update { it.copy(done = true) }
            return
        }
        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            repository.accept()
                .onSuccess { _uiState.update { it.copy(isSaving = false, done = true, acceptedAt = Instant.now(), message = "Terima kasih, Bunda.") } }
                .onFailure { e -> _uiState.update { it.copy(isSaving = false, message = e.message ?: "Gagal menyimpan persetujuan.") } }
        }
    }

    fun messageShown() = _uiState.update { it.copy(message = null) }
}

private val dateFormat = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.forLanguageTag("id-ID"))

/** Privasi & disclaimer (dari Profil). Persetujuan disimpan di server (tabel consents). */
@Composable
fun PrivacyScreen(
    onBack: () -> Unit,
    viewModel: PrivacyViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    LaunchedEffect(state.message) {
        state.message?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.messageShown()
        }
    }
    LaunchedEffect(state.done) { if (state.done) onBack() }

    AnemomScreen(title = "Privasi & disclaimer", subtitle = "Data terlindungi, Bunda lebih memahami", onBack = onBack) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Box(
                Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(Pink100),
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Outlined.GppGood, contentDescription = null, tint = Pink500, modifier = Modifier.size(36.dp)) }
        }
        Spacer(Modifier.height(14.dp))
        Text(
            text = "Peduli data Bunda,\nutamakan keselamatan",
            fontSize = 26.sp,
            lineHeight = 34.sp,
            fontWeight = FontWeight.Bold,
            color = Maroon,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "Informasi privasi · versi ${ConsentRepository.VERSION}",
            style = MaterialTheme.typography.labelMedium,
            color = MutedText,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        SectionTitle("Perlindungan data pribadi")
        AnemomCard(Modifier.fillMaxWidth()) {
            Point(
                Icons.Outlined.Lock, "Data yang Bunda simpan",
                "Nama, email, data kehamilan, hasil Hb laboratorium, foto mata, dan rekaman PPG digunakan sesuai fitur yang Bunda pilih."
            )
            Spacer(Modifier.height(14.dp))
            Point(
                Icons.Outlined.GppGood, "Penggunaan data kesehatan",
                "Data membantu pencatatan riwayat, pengingat, dan skrining awal. Akses dibatasi sesuai kewenangan; data tidak ditampilkan ke pengguna lain."
            )
            Spacer(Modifier.height(14.dp))
            Point(
                Icons.Outlined.Person, "Bunda tetap punya kendali",
                "Perbarui data melalui Edit Profil. Minta penghapusan akun atau data melalui bantuan Anemom. Bagikan hasil hanya dengan pihak yang Bunda pilih."
            )
        }

        SectionTitle("Batasan hasil skrining")
        AnemomCard(Modifier.fillMaxWidth(), containerColor = Pink100.copy(alpha = 0.75f)) {
            Point(
                Icons.Outlined.RemoveRedEye, "AI / PPG bukan diagnosis",
                "AI menghasilkan skor skrining awal. PPG merekam sinyal optik aliran darah di bawah kulit. Keduanya tidak mengukur kadar Hb darah."
            )
            Spacer(Modifier.height(14.dp))
            Point(
                Icons.Outlined.WaterDrop, "Konfirmasi dengan laboratorium",
                "Diagnosis anemia memerlukan tes Hb darah dan evaluasi bidan atau dokter. Kualitas foto, gerakan, dan sensor dapat memengaruhi skrining."
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "Jangan mengubah dosis TTD atau menunda pemeriksaan hanya berdasarkan hasil Anemom.",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = Maroon
            )
        }

        Spacer(Modifier.height(14.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(AmberBg)
                .padding(14.dp)
        ) {
            Icon(Icons.Outlined.WarningAmber, contentDescription = null, tint = AmberText, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
            Text(
                "Sesak, nyeri dada, lemas berat, atau pingsan? Segera cari pertolongan medis di IGD terdekat, apa pun hasil skrining.",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = AmberText
            )
        }

        Spacer(Modifier.height(14.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .clickable { viewModel.setChecked(!state.checked) },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = state.checked,
                onCheckedChange = viewModel::setChecked,
                colors = CheckboxDefaults.colors(checkedColor = Pink500)
            )
            Text(
                "Saya memahami penggunaan data kesehatan dan batasan skrining awal Anemom.",
                style = MaterialTheme.typography.bodySmall,
                color = Maroon
            )
        }
        state.acceptedAt?.let {
            Text(
                "Disetujui pada ${it.atZone(ZoneId.systemDefault()).format(dateFormat)}.",
                style = MaterialTheme.typography.labelSmall,
                color = MutedText,
                modifier = Modifier.padding(start = 12.dp)
            )
        }
        Spacer(Modifier.height(12.dp))
        GradientButton(
            text = if (state.acceptedAt != null) "Kembali" else "Saya Mengerti",
            onClick = viewModel::accept,
            modifier = Modifier.fillMaxWidth(),
            enabled = state.checked && !state.isSaving,
            isLoading = state.isSaving
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Spacer(Modifier.height(22.dp))
    Text(text, style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp), fontWeight = FontWeight.Bold, color = Maroon)
    Spacer(Modifier.height(10.dp))
}

@Composable
private fun Point(icon: ImageVector, title: String, body: String) {
    Row {
        Box(
            Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Pink100),
            contentAlignment = Alignment.Center
        ) { Icon(icon, contentDescription = null, tint = Pink500, modifier = Modifier.size(20.dp)) }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Maroon)
            Text(body, style = MaterialTheme.typography.bodySmall, color = MutedText)
        }
    }
}

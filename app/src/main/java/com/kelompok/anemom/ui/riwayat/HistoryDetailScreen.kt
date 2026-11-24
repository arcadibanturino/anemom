package com.kelompok.anemom.ui.riwayat

import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.MedicalServices
import androidx.compose.material.icons.outlined.Medication
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kelompok.anemom.data.repository.HbRepository
import com.kelompok.anemom.data.repository.ScreeningRepository
import com.kelompok.anemom.ui.AppViewModelProvider
import com.kelompok.anemom.ui.components.AnemomCard
import com.kelompok.anemom.ui.components.AnemomChip
import com.kelompok.anemom.ui.components.AnemomOutlinedButton
import com.kelompok.anemom.ui.components.AnemomScreen
import com.kelompok.anemom.ui.components.GradientButton
import com.kelompok.anemom.ui.theme.Maroon
import com.kelompok.anemom.ui.theme.MutedText
import com.kelompok.anemom.ui.theme.Pink100
import com.kelompok.anemom.ui.theme.Pink500
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

const val HISTORY_ARG_KIND = "kind"
const val HISTORY_ARG_ID = "id"

data class HistoryDetailUiState(
    val isLoading: Boolean = true,
    val detail: HistoryDetail? = null,
    val error: String? = null
)

class HistoryDetailViewModel(
    savedStateHandle: SavedStateHandle,
    private val hbRepository: HbRepository,
    private val screeningRepository: ScreeningRepository
) : ViewModel() {

    private val kind = HistoryKind.fromRoute(savedStateHandle.get<String>(HISTORY_ARG_KIND))
    private val id: Long = savedStateHandle.get<Long>(HISTORY_ARG_ID) ?: 0L

    private val _uiState = MutableStateFlow(HistoryDetailUiState())
    val uiState: StateFlow<HistoryDetailUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        _uiState.value = HistoryDetailUiState(isLoading = true)
        viewModelScope.launch {
            val result: Result<HistoryItem?> = when (kind) {
                HistoryKind.HB -> hbRepository.list().map { list -> list.firstOrNull { it.id == id }?.let { HistoryItem.Hb(it) } }
                HistoryKind.AI, HistoryKind.PPG ->
                    screeningRepository.recent(limit = 50).map { list -> list.firstOrNull { it.id == id }?.let { HistoryItem.Scan(it) } }
                null -> Result.success(null)
            }
            _uiState.value = result.fold(
                onSuccess = { item ->
                    if (item == null) {
                        HistoryDetailUiState(isLoading = false, error = "Pemeriksaan tidak ditemukan.")
                    } else {
                        HistoryDetailUiState(isLoading = false, detail = item.toDetail())
                    }
                },
                onFailure = { HistoryDetailUiState(isLoading = false, error = it.message ?: "Gagal memuat detail.") }
            )
        }
    }
}

/** Detail riwayat: satu pemeriksaan (Hb lab, AI Scan Mata, atau PPG) + ekspor PDF. */
@Composable
fun HistoryDetailScreen(
    onBack: () -> Unit,
    viewModel: HistoryDetailViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val detail = state.detail

    AnemomScreen(title = "Detail riwayat", subtitle = detail?.item?.title(), onBack = onBack) {
        when {
            state.isLoading -> Box(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 48.dp),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator(color = Pink500) }

            detail == null -> {
                Spacer(Modifier.height(16.dp))
                Text(state.error.orEmpty(), style = MaterialTheme.typography.bodyMedium, color = MutedText)
                Spacer(Modifier.height(12.dp))
                AnemomOutlinedButton("Coba lagi", onClick = viewModel::load, modifier = Modifier.fillMaxWidth())
            }

            else -> {
                AnemomCard(Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = detail.headline,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Maroon,
                            modifier = Modifier.weight(1f)
                        )
                        AnemomChip(detail.badge, containerColor = Pink100, contentColor = Pink500)
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(detail.value, fontSize = 40.sp, fontWeight = FontWeight.ExtraBold, color = Maroon)
                        detail.unit?.let {
                            Text(" $it", style = MaterialTheme.typography.bodyLarge, color = MutedText, modifier = Modifier.padding(bottom = 6.dp))
                        }
                    }
                    detail.status?.let {
                        Spacer(Modifier.height(8.dp))
                        AnemomChip(it.text, containerColor = it.container, contentColor = it.content)
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(detail.chipNote, style = MaterialTheme.typography.bodySmall, color = MutedText)
                }

                Spacer(Modifier.height(14.dp))
                AnemomCard(Modifier.fillMaxWidth()) {
                    Text("Rincian pemeriksaan", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Maroon)
                    detail.rows.forEach { (label, value) ->
                        Spacer(Modifier.height(10.dp))
                        Text(label, style = MaterialTheme.typography.labelSmall, color = MutedText)
                        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = Maroon)
                    }
                }

                Spacer(Modifier.height(20.dp))
                Text(
                    text = "Rekomendasi untuk Bunda",
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp),
                    fontWeight = FontWeight.Bold,
                    color = Maroon
                )
                Spacer(Modifier.height(10.dp))
                AnemomCard(Modifier.fillMaxWidth()) {
                    detail.advice.forEachIndexed { i, (head, text) ->
                        if (i > 0) Spacer(Modifier.height(14.dp))
                        Row {
                            Box(
                                Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Pink100),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (head.contains("TTD")) Icons.Outlined.Medication else Icons.Outlined.MedicalServices,
                                    contentDescription = null,
                                    tint = Pink500,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(head, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Maroon)
                                Text(text, style = MaterialTheme.typography.bodySmall, color = MutedText)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Pink100.copy(alpha = 0.75f))
                        .padding(14.dp)
                ) {
                    Icon(Icons.Outlined.Info, contentDescription = null, tint = Pink500, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(detail.footnote, style = MaterialTheme.typography.bodySmall, color = Maroon)
                }

                Spacer(Modifier.height(18.dp))
                GradientButton(
                    text = "Ekspor PDF",
                    onClick = {
                        runCatching { HistoryPdf.share(context, detail, motherName = null) }
                            .onFailure { Toast.makeText(context, "PDF gagal dibuat.", Toast.LENGTH_SHORT).show() }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    icon = Icons.Outlined.Download
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "PDF memuat sumber, tanggal, dan catatan bahwa hasil skrining bukan diagnosis.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MutedText,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

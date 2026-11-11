package com.kelompok.anemom.ui.riwayat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kelompok.anemom.data.model.HbPoint
import com.kelompok.anemom.data.model.HbResult
import com.kelompok.anemom.data.model.HbRisk
import com.kelompok.anemom.data.repository.HbRepository
import com.kelompok.anemom.util.DateFormats
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class RiwayatUiState(
    val isLoaded: Boolean = false,
    /** Titik grafik, tanggal terlama ke terbaru (maks. 8 terakhir). */
    val chartPoints: List<HbPoint> = emptyList(),
    /** Daftar hasil, terbaru lebih dulu. */
    val results: List<HbResult> = emptyList(),
    val latest: HbResult? = null,
    val latestRisk: HbRisk? = null
)

class RiwayatViewModel(repository: HbRepository) : ViewModel() {

    val uiState: StateFlow<RiwayatUiState> = repository.results.map { list ->
        val sorted = list.sortedBy { it.date }
        RiwayatUiState(
            isLoaded = true,
            chartPoints = sorted.takeLast(8).map { HbPoint(DateFormats.dayMonth(it.date), it.value) },
            results = sorted.reversed(),
            latest = sorted.lastOrNull(),
            latestRisk = sorted.lastOrNull()?.let { HbRisk.classify(it.value) }
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = RiwayatUiState()
    )
}

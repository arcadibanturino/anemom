package com.kelompok.anemom.ui.riwayat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kelompok.anemom.data.repository.HbRepository
import com.kelompok.anemom.data.repository.Screening
import com.kelompok.anemom.data.repository.ScreeningRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import java.time.YearMonth

enum class HistoryFilter(val label: String) {
    ALL("Semua"), HB("Hb darah"), AI_PPG("AI / PPG")
}

data class HistoryMonth(val month: YearMonth, val items: List<HistoryItem>)

data class RiwayatUiState(
    val isLoaded: Boolean = false,
    val selectedFilter: HistoryFilter = HistoryFilter.ALL,
    /** Bulan terbaru dulu; di dalamnya item terbaru dulu. */
    val months: List<HistoryMonth> = emptyList()
)

/** Tab Riwayat: semua pemeriksaan (Hb lab, AI Scan Mata, PPG) dikelompokkan per bulan. */
class RiwayatViewModel(
    hbRepository: HbRepository,
    screeningRepository: ScreeningRepository
) : ViewModel() {

    private val filter = MutableStateFlow(HistoryFilter.ALL)

    // Skrining boleh gagal dimuat tanpa menghalangi daftar Hb.
    private val screenings = screeningRepository.history.onStart { emit(emptyList<Screening>()) }

    val uiState: StateFlow<RiwayatUiState> = combine(hbRepository.results, screenings, filter) { hb, scans, f ->
        val all = hb.map { HistoryItem.Hb(it) } + scans.map { HistoryItem.Scan(it) }
        val shown = all.filter {
            when (f) {
                HistoryFilter.ALL -> true
                HistoryFilter.HB -> it.kind == HistoryKind.HB
                HistoryFilter.AI_PPG -> it.kind != HistoryKind.HB
            }
        }.sortedByDescending { it.at }
        RiwayatUiState(
            isLoaded = true,
            selectedFilter = f,
            months = shown.groupBy { YearMonth.from(it.at) }.map { (m, items) -> HistoryMonth(m, items) }
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = RiwayatUiState()
    )

    fun setFilter(value: HistoryFilter) {
        filter.value = value
    }
}

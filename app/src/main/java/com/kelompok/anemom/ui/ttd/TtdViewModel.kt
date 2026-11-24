package com.kelompok.anemom.ui.ttd

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kelompok.anemom.data.repository.TtdRepository
import com.kelompok.anemom.data.repository.TtdTrackerData
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.YearMonth

data class TtdUiState(
    /** Null selama data pertama belum datang. */
    val data: TtdTrackerData? = null,
    val isMarking: Boolean = false
)

@OptIn(ExperimentalCoroutinesApi::class)
class TtdViewModel(private val repository: TtdRepository) : ViewModel() {

    /** Bulan kalender yang dipilih; riwayat dan grafik selalu berpatokan pada hari ini. */
    private val selectedMonth = MutableStateFlow(YearMonth.now())
    private val marking = MutableStateFlow(false)

    val uiState: StateFlow<TtdUiState> = combine(
        selectedMonth.flatMapLatest { repository.tracker(it) },
        marking
    ) { data, isMarking -> TtdUiState(data, isMarking) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = TtdUiState()
        )

    fun previousMonth() = selectedMonth.update { it.minusMonths(1) }

    fun nextMonth() = selectedMonth.update { it.plusMonths(1) }

    /** Berhasil -> data dimuat ulang dan alarm hari ini dihentikan; gagal -> Toast dari repository. */
    fun markTakenToday() {
        if (marking.value) return
        marking.value = true
        viewModelScope.launch {
            repository.markTakenToday()
            marking.value = false
        }
    }
}

package com.kelompok.anemom.ui.ttd

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kelompok.anemom.data.model.TtdEntry
import com.kelompok.anemom.data.repository.TtdRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import kotlin.math.roundToInt

data class TtdUiState(
    val reminderOn: Boolean = true,
    val takenToday: Boolean = false,
    val today: LocalDate = LocalDate.now(),
    val takenDates: Set<LocalDate> = emptySet(),
    /** Riwayat terbaru lebih dulu. */
    val history: List<TtdEntry> = emptyList(),
    val takenLast7Days: Int = 0,
    val percentLast7Days: Int = 0,
    val month: YearMonth = YearMonth.now()
)

class TtdViewModel(private val repository: TtdRepository) : ViewModel() {

    private val month = MutableStateFlow(YearMonth.now())

    val uiState: StateFlow<TtdUiState> = combine(
        repository.entries,
        repository.reminderOn,
        month
    ) { entries, reminderOn, shownMonth ->
        val today = LocalDate.now()
        val takenDates = entries.map { it.date }.toSet()
        val windowStart = today.minusDays(6)
        val last7 = takenDates.count { !it.isBefore(windowStart) && !it.isAfter(today) }

        TtdUiState(
            reminderOn = reminderOn,
            takenToday = today in takenDates,
            today = today,
            takenDates = takenDates,
            history = entries.sortedWith(compareByDescending<TtdEntry> { it.date }.thenByDescending { it.time }),
            takenLast7Days = last7,
            percentLast7Days = (last7 * 100f / 7f).roundToInt(),
            month = shownMonth
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = TtdUiState()
    )

    fun previousMonth() = month.update { it.minusMonths(1) }

    fun nextMonth() = month.update { it.plusMonths(1) }

    fun markTakenToday() {
        viewModelScope.launch { repository.markTakenToday() }
    }

    fun setReminder(on: Boolean) {
        viewModelScope.launch { repository.setReminder(on) }
    }
}

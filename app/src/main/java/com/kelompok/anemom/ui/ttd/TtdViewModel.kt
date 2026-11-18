package com.kelompok.anemom.ui.ttd

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kelompok.anemom.data.model.TtdEntry
import com.kelompok.anemom.data.repository.TtdRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

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

@OptIn(ExperimentalCoroutinesApi::class)
class TtdViewModel(private val repository: TtdRepository) : ViewModel() {

    /** Bulan yang dipilih pengguna; data bulannya diambil dari server. */
    private val selectedMonth = MutableStateFlow(YearMonth.now())

    val uiState: StateFlow<TtdUiState> = combine(
        selectedMonth.flatMapLatest { repository.month(it) },
        repository.reminderOn
    ) { data, reminderOn ->
        TtdUiState(
            reminderOn = reminderOn,
            takenToday = data.takenToday,
            today = LocalDate.now(),
            takenDates = data.entries.map { it.date }.toSet(),
            history = data.history,
            // Kepatuhan 7 hari terakhir dihitung server dan tidak bergantung pada bulan yang dilihat.
            takenLast7Days = data.takenLast7Days,
            percentLast7Days = data.percentLast7Days,
            // Bulan ikut data yang sudah dimuat, supaya kalender tidak menampilkan
            // tanda minum bulan lama pada bulan baru selama menunggu server.
            month = data.month
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = TtdUiState()
    )

    fun previousMonth() = selectedMonth.update { it.minusMonths(1) }

    fun nextMonth() = selectedMonth.update { it.plusMonths(1) }

    /** Hasilnya (gagal/berhasil) ditangani repository: berhasil -> data dimuat ulang, gagal -> Toast. */
    fun markTakenToday() {
        viewModelScope.launch { repository.markTakenToday() }
    }

    fun setReminder(on: Boolean) {
        viewModelScope.launch { repository.setReminder(on) }
    }
}

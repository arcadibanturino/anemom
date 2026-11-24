package com.kelompok.anemom.alarm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kelompok.anemom.data.repository.TtdRepository
import com.kelompok.anemom.data.repository.TtdSummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.time.ZonedDateTime

enum class AlarmStep { RINGING, SNOOZE_PICK, SNOOZED, DONE }

data class AlarmUiState(
    val step: AlarmStep = AlarmStep.RINGING,
    /** Bunda menggeser "Belum Diminum". */
    val reportedNotTaken: Boolean = false,
    val snoozedUntil: ZonedDateTime? = null,
    val takenAt: LocalTime? = null,
    val summary: TtdSummary? = null,
    val isSaving: Boolean = false,
    val error: String? = null
)

/**
 * Alur layar alarm: berbunyi -> (Sudah) tercatat, atau (Belum / Tunda) pilih tunda -> ditunda.
 * [demo] = pratinjau atau alarm uji: tidak menyimpan ke server dan tidak mengubah jadwal.
 */
class AlarmViewModel(
    private val repository: TtdRepository,
    private val scheduler: TtdAlarmScheduler,
    private val stopRinging: () -> Unit,
    val demo: Boolean
) : ViewModel() {

    private val _uiState = MutableStateFlow(AlarmUiState())
    val uiState: StateFlow<AlarmUiState> = _uiState.asStateFlow()

    fun confirmTaken() {
        if (_uiState.value.isSaving) return
        stopRinging()
        _uiState.update { it.copy(isSaving = true, error = null) }
        viewModelScope.launch {
            val result = if (demo) Result.success(Unit) else repository.markTakenToday()
            result
                .onSuccess {
                    _uiState.update {
                        it.copy(step = AlarmStep.DONE, isSaving = false, takenAt = LocalTime.now())
                    }
                    loadSummary()
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(isSaving = false, error = e.message ?: "Gagal menyimpan. Periksa internet lalu coba lagi.")
                    }
                }
        }
    }

    /** Geser "Belum Diminum": dicatat Belum di server, lalu pilih kapan diingatkan lagi. */
    fun reportNotTaken() {
        stopRinging()
        _uiState.update { it.copy(step = AlarmStep.SNOOZE_PICK, reportedNotTaken = true, error = null) }
        if (!demo) {
            viewModelScope.launch { repository.markNotTakenToday() }
        }
    }

    /** Tombol "Tunda Pengingat": tidak mencatat apa pun. */
    fun openSnooze() {
        stopRinging()
        _uiState.update { it.copy(step = AlarmStep.SNOOZE_PICK, error = null) }
    }

    fun snooze(minutes: Int) {
        val at = if (demo) {
            ZonedDateTime.now().plusMinutes(minutes.toLong()).withSecond(0).withNano(0)
        } else {
            scheduler.snooze(minutes)
        }
        _uiState.update { it.copy(step = AlarmStep.SNOOZED, snoozedUntil = at) }
        loadSummary()
    }

    /** "Kembali ke alarm": kembali ke layar utama alarm (nada sudah berhenti; tunda tetap berlaku). */
    fun backToAlarm() {
        _uiState.update { it.copy(step = AlarmStep.RINGING, error = null) }
    }

    private fun loadSummary() {
        if (demo) {
            _uiState.update { it.copy(summary = TtdSummary(5, 71, it.step == AlarmStep.DONE)) }
            return
        }
        viewModelScope.launch {
            repository.summary().onSuccess { s -> _uiState.update { it.copy(summary = s) } }
        }
    }
}

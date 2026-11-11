package com.kelompok.anemom.ui.skrining

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class WatchStage { IDLE, RECORDING, DONE }

data class SmartwatchUiState(
    val stage: WatchStage = WatchStage.IDLE,
    val secondsLeft: Int = RECORD_SECONDS,
    val heartRate: Int? = null,
    val spo2: Int? = null,
    val signalQuality: String? = null
)

const val RECORD_SECONDS = 60

/**
 * SIMULASI. Belum ada koneksi BLE ke band dan belum ada model PPG.
 * TODO(fitur): sambungkan ke band lewat BLE, rekam sinyal PPG asli, kirim ke model estimasi Hb.
 */
class SmartwatchViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(SmartwatchUiState())
    val uiState: StateFlow<SmartwatchUiState> = _uiState.asStateFlow()

    private var job: Job? = null

    fun startRecording() {
        if (_uiState.value.stage == WatchStage.RECORDING) return
        job?.cancel()
        job = viewModelScope.launch {
            _uiState.value = SmartwatchUiState(stage = WatchStage.RECORDING)
            // Simulasi 60 detik dipercepat: 1 detik asli = 10 detik rekam (selesai dalam 6 detik).
            repeat(RECORD_SECONDS / 10) { tick ->
                delay(1_000)
                _uiState.update { it.copy(secondsLeft = RECORD_SECONDS - (tick + 1) * 10) }
            }
            _uiState.value = SmartwatchUiState(
                stage = WatchStage.DONE,
                secondsLeft = 0,
                heartRate = (78..92).random(),
                spo2 = (96..99).random(),
                signalQuality = "Baik"
            )
        }
    }

    fun reset() {
        job?.cancel()
        _uiState.value = SmartwatchUiState()
    }
}

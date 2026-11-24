package com.kelompok.anemom.ui.skrining.smartband

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kelompok.anemom.data.smartband.PpgQuality
import com.kelompok.anemom.data.smartband.PpgRecording
import com.kelompok.anemom.data.smartband.SmartbandInfo
import com.kelompok.anemom.data.smartband.SmartbandRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Hitung mundur sebelum merekam (detik). */
const val PREPARE_SECONDS = 10

/** Lama rekaman PPG (detik). */
const val RECORD_SECONDS = 60

/** Lebar potongan gelombang yang tampil di layar (detik). */
private const val WINDOW_SECONDS = 4

enum class PpgStage { PREPARING, RECORDING, DONE, UPLOADING, UPLOADED, ERROR }

data class PpgRecordUiState(
    val stage: PpgStage = PpgStage.PREPARING,
    val isSimulated: Boolean = false,
    val device: SmartbandInfo? = null,
    val countdown: Int = PREPARE_SECONDS,
    val recordedSeconds: Int = 0,
    /** Sampel kanal merah terakhir (maks. [waveCapacity]) untuk digambar. */
    val wave: List<Float> = emptyList(),
    val waveCapacity: Int = 0,
    val quality: PpgQuality? = null,
    val errorMessage: String? = null,
    val recordingId: String? = null
) {
    val progress: Float get() = recordedSeconds / RECORD_SECONDS.toFloat()
}

/**
 * Rekam PPG: hitung mundur [PREPARE_SECONDS] detik, lalu rekam [RECORD_SECONDS] detik
 * sambil menampilkan gelombang, lalu kirim ke server. Mulai otomatis saat layar dibuka.
 */
class PpgRecordViewModel(
    private val repository: SmartbandRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        PpgRecordUiState(
            isSimulated = repository.isSimulated,
            device = repository.connectedDevice.value
        )
    )
    val uiState: StateFlow<PpgRecordUiState> = _uiState.asStateFlow()

    private var recordJob: Job? = null
    private var recording: PpgRecording? = null

    init {
        viewModelScope.launch {
            repository.connectedDevice.collect { d -> _uiState.update { it.copy(device = d) } }
        }
        start()
    }

    /** Mulai (atau ulangi) dari hitung mundur. */
    fun start() {
        recordJob?.cancel()
        recording = null
        recordJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    stage = PpgStage.PREPARING,
                    countdown = PREPARE_SECONDS,
                    recordedSeconds = 0,
                    wave = emptyList(),
                    quality = null,
                    errorMessage = null,
                    recordingId = null
                )
            }

            val device = repository.connectedDevice.value
            if (device == null) {
                fail("Smartband belum tersambung. Sambungkan dulu di Koneksi Bluetooth.")
                return@launch
            }

            for (s in PREPARE_SECONDS downTo 1) {
                _uiState.update { it.copy(countdown = s) }
                delay(1_000)
            }

            val rate = repository.sampleRateHz
            val target = rate * RECORD_SECONDS
            val window = rate * WINDOW_SECONDS
            val red = ArrayList<Float>(target)
            val ir = ArrayList<Float>(target)
            val startedAt = System.currentTimeMillis()
            _uiState.update { it.copy(stage = PpgStage.RECORDING, waveCapacity = window) }

            try {
                repository.streamPpg()
                    .takeWhile { red.size < target }
                    .collect { chunk ->
                        val take = minOf(chunk.red.size, target - red.size)
                        red.addAll(chunk.red.subList(0, take))
                        ir.addAll(chunk.ir.subList(0, minOf(take, chunk.ir.size)))
                        _uiState.update {
                            it.copy(
                                recordedSeconds = red.size / rate,
                                wave = red.takeLast(window),
                                quality = chunk.quality
                            )
                        }
                    }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                fail(e.message ?: "Rekaman terhenti.")
                return@launch
            }

            recording = PpgRecording(
                deviceId = device.id,
                sampleRateHz = rate,
                startedAtMillis = startedAt,
                red = red.toFloatArray(),
                ir = ir.toFloatArray()
            )
            _uiState.update { it.copy(stage = PpgStage.DONE, recordedSeconds = RECORD_SECONDS) }
        }
    }

    /** Kirim rekaman ke server. Masih simulasi sampai endpoint PPG tersedia. */
    fun upload() {
        val rec = recording ?: return
        if (_uiState.value.stage != PpgStage.DONE) return
        viewModelScope.launch {
            _uiState.update { it.copy(stage = PpgStage.UPLOADING, errorMessage = null) }
            repository.uploadRecording(rec)
                .onSuccess { id -> _uiState.update { it.copy(stage = PpgStage.UPLOADED, recordingId = id) } }
                .onFailure { e ->
                    _uiState.update { it.copy(stage = PpgStage.DONE, errorMessage = "Gagal mengirim: ${e.message}") }
                }
        }
    }

    private fun fail(message: String) {
        _uiState.update { it.copy(stage = PpgStage.ERROR, errorMessage = message) }
    }
}

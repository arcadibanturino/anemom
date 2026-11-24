package com.kelompok.anemom.ui.skrining.smartband

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kelompok.anemom.data.smartband.SmartbandInfo
import com.kelompok.anemom.data.smartband.SmartbandRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SmartbandConnectUiState(
    val isSimulated: Boolean = false,
    val bluetoothEnabled: Boolean = false,
    val connected: SmartbandInfo? = null,
    /** Semua hasil pindai terakhir (bisa termasuk yang sedang tersambung). */
    val found: List<SmartbandInfo> = emptyList(),
    val isScanning: Boolean = false,
    val connectingId: String? = null,
    val isDisconnecting: Boolean = false,
    val message: String? = null
) {
    /** Perangkat di sekitar selain yang sedang tersambung. */
    val available: List<SmartbandInfo> get() = found.filterNot { it.id == connected?.id }
}

/** Layar Koneksi Bluetooth. Semua akses perangkat lewat [SmartbandRepository]. */
class SmartbandConnectViewModel(
    private val repository: SmartbandRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SmartbandConnectUiState(isSimulated = repository.isSimulated))
    val uiState: StateFlow<SmartbandConnectUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(repository.bluetoothEnabled, repository.connectedDevice) { bt, device -> bt to device }
                .collect { (bt, device) ->
                    _uiState.update {
                        it.copy(
                            bluetoothEnabled = bt,
                            connected = device,
                            found = if (bt) it.found else emptyList()
                        )
                    }
                }
        }
        scan()
    }

    fun scan() {
        if (_uiState.value.isScanning || !repository.bluetoothEnabled.value) return
        viewModelScope.launch {
            _uiState.update { it.copy(isScanning = true) }
            try {
                val devices = repository.scan()
                _uiState.update { it.copy(found = devices) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(message = "Gagal mencari perangkat: ${e.message}") }
            } finally {
                _uiState.update { it.copy(isScanning = false) }
            }
        }
    }

    fun setBluetoothEnabled(enabled: Boolean) {
        repository.setBluetoothEnabled(enabled)
        if (enabled) scan()
    }

    fun connect(device: SmartbandInfo) {
        if (_uiState.value.connectingId != null) return
        viewModelScope.launch {
            _uiState.update { it.copy(connectingId = device.id) }
            repository.connect(device.id)
                .onSuccess { d -> _uiState.update { it.copy(message = "${d.name} tersambung.") } }
                .onFailure { e -> _uiState.update { it.copy(message = e.message ?: "Gagal menghubungkan.") } }
            _uiState.update { it.copy(connectingId = null) }
        }
    }

    fun disconnect() {
        if (_uiState.value.isDisconnecting) return
        viewModelScope.launch {
            _uiState.update { it.copy(isDisconnecting = true) }
            try {
                repository.disconnect()
            } finally {
                _uiState.update { it.copy(isDisconnecting = false) }
            }
        }
    }

    fun messageShown() {
        _uiState.update { it.copy(message = null) }
    }
}

package com.kelompok.anemom.ui.skrining

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kelompok.anemom.data.repository.AuthRepository
import com.kelompok.anemom.data.repository.HbRepository
import com.kelompok.anemom.util.PregnancyCalculator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

data class InputHbUiState(
    val motherName: String = "",
    val pregnancyWeek: Int? = null,
    val hbText: String = "",
    val date: LocalDate = LocalDate.now(),
    val facility: String = "",
    val hbError: String? = null,
    val isSaving: Boolean = false,
    val saved: Boolean = false
)

class InputHbViewModel(
    private val hbRepository: HbRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(InputHbUiState())
    val uiState: StateFlow<InputHbUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val account = authRepository.account.first() ?: return@launch
            val week = runCatching {
                PregnancyCalculator.weeksSince(LocalDate.parse(account.hpht))
            }.getOrNull()
            _uiState.update { it.copy(motherName = account.name, pregnancyWeek = week) }
        }
    }

    fun onHbChange(value: String) {
        // Hanya angka dan satu pemisah desimal (titik atau koma), maksimal 4 karakter.
        val cleaned = value.filter { it.isDigit() || it == '.' || it == ',' }
        if (cleaned.length <= 4 && cleaned.count { it == '.' || it == ',' } <= 1) {
            _uiState.update { it.copy(hbText = cleaned, hbError = null) }
        }
    }

    fun onDateChange(date: LocalDate) = _uiState.update { it.copy(date = date) }

    fun onFacilityChange(value: String) = _uiState.update { it.copy(facility = value) }

    fun save() {
        val state = _uiState.value
        val hb = state.hbText.replace(',', '.').toFloatOrNull()
        if (hb == null || hb < MIN_HB || hb > MAX_HB) {
            _uiState.update { it.copy(hbError = "Masukkan Hb antara ${MIN_HB.toInt()} dan ${MAX_HB.toInt()} g/dL") }
            return
        }
        if (state.date.isAfter(LocalDate.now())) {
            _uiState.update { it.copy(hbError = "Tanggal tes tidak boleh di masa depan") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            hbRepository.add(hb, state.date, state.facility, SOURCE_LAB)
            _uiState.update { it.copy(isSaving = false, saved = true) }
        }
    }

    fun consumeSaved() = _uiState.update { it.copy(saved = false, hbText = "", facility = "") }

    companion object {
        const val MIN_HB = 3f
        const val MAX_HB = 20f
        const val SOURCE_LAB = "Laboratorium"
    }
}

package com.kelompok.anemom.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kelompok.anemom.util.Validators
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ForgotPasswordUiState(
    val email: String = "",
    val emailError: String? = null,
    val isLoading: Boolean = false,
    val isSent: Boolean = false,       // false = form, true = layar "Periksa email Bunda"
    val resendCooldown: Int = 0        // detik tersisa sebelum boleh kirim ulang
)

class ForgotPasswordViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(ForgotPasswordUiState())
    val uiState: StateFlow<ForgotPasswordUiState> = _uiState.asStateFlow()

    private var cooldownJob: Job? = null

    fun onEmailChange(value: String) =
        _uiState.update { it.copy(email = value, emailError = null) }

    fun sendResetLink() {
        val email = _uiState.value.email.trim()
        if (!Validators.isValidEmail(email)) {
            _uiState.update { it.copy(emailError = "Format email tidak valid") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, email = email) }
            // TODO(backend): panggil API/Firebase kirim email reset di sini.
            delay(800) // simulasi jaringan
            _uiState.update { it.copy(isLoading = false, isSent = true) }
            startCooldown()
        }
    }

    fun resend() {
        if (_uiState.value.resendCooldown > 0) return
        // TODO(backend): kirim ulang email reset.
        startCooldown()
    }

    /** "Ubah alamat email": kembali ke form dengan email tetap terisi. */
    fun editEmail() {
        cooldownJob?.cancel()
        _uiState.update { it.copy(isSent = false, resendCooldown = 0) }
    }

    private fun startCooldown() {
        cooldownJob?.cancel()
        cooldownJob = viewModelScope.launch {
            for (s in 30 downTo 1) {
                _uiState.update { it.copy(resendCooldown = s) }
                delay(1000)
            }
            _uiState.update { it.copy(resendCooldown = 0) }
        }
    }
}
package com.kelompok.anemom.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kelompok.anemom.data.local.UserAccount
import com.kelompok.anemom.data.repository.AuthRepository
import com.kelompok.anemom.util.PregnancyCalculator
import com.kelompok.anemom.util.Validators
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

data class RegisterUiState(
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val hpht: LocalDate? = null,
    val trimester: Int? = null,
    val agreed: Boolean = false,
    val nameError: String? = null,
    val emailError: String? = null,
    val phoneError: String? = null,
    val passwordError: String? = null,
    val confirmError: String? = null,
    val hphtError: String? = null,
    val trimesterError: String? = null,
    val errorMessage: String? = null,
    val isLoading: Boolean = false
) {
    /** Usia kehamilan (minggu) dihitung otomatis dari HPHT. */
    val pregnancyWeeks: Int?
        get() = hpht?.let { PregnancyCalculator.weeksSince(it) }
}

class RegisterViewModel(private val authRepository: AuthRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    fun onNameChange(v: String) = _uiState.update { it.copy(name = v, nameError = null, errorMessage = null) }
    fun onEmailChange(v: String) = _uiState.update { it.copy(email = v, emailError = null, errorMessage = null) }
    fun onPhoneChange(v: String) = _uiState.update { it.copy(phone = v, phoneError = null, errorMessage = null) }
    fun onPasswordChange(v: String) = _uiState.update { it.copy(password = v, passwordError = null, errorMessage = null) }
    fun onConfirmPasswordChange(v: String) =
        _uiState.update { it.copy(confirmPassword = v, confirmError = null, errorMessage = null) }

    fun onAgreedChange(v: Boolean) = _uiState.update { it.copy(agreed = v, errorMessage = null) }

    /** Setelah HPHT dipilih, trimester diisi otomatis (masih bisa diubah manual). */
    fun onHphtSelected(date: LocalDate) = _uiState.update {
        val weeks = PregnancyCalculator.weeksSince(date)
        it.copy(
            hpht = date,
            trimester = PregnancyCalculator.trimesterFor(weeks),
            hphtError = null,
            trimesterError = null,
            errorMessage = null
        )
    }

    fun onTrimesterSelected(trimester: Int) =
        _uiState.update { it.copy(trimester = trimester, trimesterError = null) }

    fun register(onSuccess: () -> Unit) {
        val s = _uiState.value

        val nameError = if (s.name.isBlank()) "Nama lengkap wajib diisi" else null
        val emailError = if (!Validators.isValidEmail(s.email)) "Format email tidak valid" else null
        val phoneError = if (!Validators.isValidPhone(s.phone)) "Nomor telepon tidak valid" else null
        val passwordError = if (!Validators.isValidPassword(s.password)) "Kata sandi minimal 8 karakter" else null
        val confirmError = if (s.confirmPassword != s.password) "Konfirmasi kata sandi tidak sama" else null
        val hpht = s.hpht
        val hphtError = when {
            hpht == null -> "Pilih tanggal HPHT"
            hpht.isAfter(LocalDate.now()) -> "Tanggal HPHT tidak boleh di masa depan"
            PregnancyCalculator.weeksSince(hpht) > PregnancyCalculator.MAX_WEEKS ->
                "HPHT lebih dari ${PregnancyCalculator.MAX_WEEKS} minggu lalu"
            else -> null
        }
        val trimesterError = if (s.trimester == null) "Pilih trimester kehamilan" else null

        val hasFieldError = listOf(
            nameError, emailError, phoneError, passwordError, confirmError, hphtError, trimesterError
        ).any { it != null }

        if (hasFieldError || !s.agreed) {
            _uiState.update {
                it.copy(
                    nameError = nameError,
                    emailError = emailError,
                    phoneError = phoneError,
                    passwordError = passwordError,
                    confirmError = confirmError,
                    hphtError = hphtError,
                    trimesterError = trimesterError,
                    errorMessage = if (!s.agreed) {
                        "Setujui Syarat Penggunaan dan Kebijakan Privasi terlebih dahulu."
                    } else {
                        null
                    }
                )
            }
            return
        }

        val account = UserAccount(
            name = s.name.trim(),
            email = s.email.trim(),
            phone = s.phone.trim(),
            passwordHash = "", // diisi (di-hash) oleh AuthRepository
            hpht = hpht?.toString().orEmpty(),
            trimester = s.trimester ?: 1
        )

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            authRepository.register(account, s.password)
                .onSuccess {
                    _uiState.update { it.copy(isLoading = false) }
                    onSuccess()
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = error.message ?: "Gagal membuat akun. Coba lagi.")
                    }
                }
        }
    }
}

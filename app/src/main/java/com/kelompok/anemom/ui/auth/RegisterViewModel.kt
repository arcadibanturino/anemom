package com.kelompok.anemom.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kelompok.anemom.data.local.UserAccount
import com.kelompok.anemom.data.repository.AuthRepository
import com.kelompok.anemom.util.AgeCalculator
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
    val password: String = "",
    val confirmPassword: String = "",
    val birthDate: LocalDate? = null,
    val hpht: LocalDate? = null,
    val agreed: Boolean = false,
    val nameError: String? = null,
    val emailError: String? = null,
    val passwordError: String? = null,
    val confirmError: String? = null,
    val birthDateError: String? = null,
    val hphtError: String? = null,
    val errorMessage: String? = null,
    val isLoading: Boolean = false
) {
    /** Usia kehamilan (minggu) dihitung otomatis dari HPHT. */
    val pregnancyWeeks: Int?
        get() = hpht?.let { PregnancyCalculator.weeksSince(it) }

    /** Trimester ditentukan otomatis dari usia kehamilan. */
    val trimester: Int?
        get() = pregnancyWeeks?.takeIf { it >= 0 }?.let { PregnancyCalculator.trimesterFor(it) }
}

class RegisterViewModel(private val authRepository: AuthRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    fun onNameChange(v: String) = _uiState.update { it.copy(name = v, nameError = null, errorMessage = null) }
    fun onEmailChange(v: String) = _uiState.update { it.copy(email = v, emailError = null, errorMessage = null) }
    fun onPasswordChange(v: String) = _uiState.update { it.copy(password = v, passwordError = null, errorMessage = null) }
    fun onConfirmPasswordChange(v: String) =
        _uiState.update { it.copy(confirmPassword = v, confirmError = null, errorMessage = null) }

    fun onAgreedChange(v: Boolean) = _uiState.update { it.copy(agreed = v, errorMessage = null) }

    fun onBirthDateSelected(date: LocalDate) = _uiState.update {
        it.copy(birthDate = date, birthDateError = null, errorMessage = null)
    }

    /** Setelah HPHT dipilih, usia kehamilan dan trimester terisi otomatis. */
    fun onHphtSelected(date: LocalDate) = _uiState.update {
        it.copy(hpht = date, hphtError = null, errorMessage = null)
    }

    fun register(onSuccess: () -> Unit) {
        val s = _uiState.value

        val nameError = when {
            s.name.isBlank() -> "Nama atau nama panggilan wajib diisi"
            s.name.trim().length < 2 -> "Nama minimal 2 karakter"   // sesuai aturan backend
            else -> null
        }
        val emailError = if (!Validators.isValidEmail(s.email)) "Format email tidak valid" else null
        val passwordError = if (!Validators.isValidPassword(s.password)) "Kata sandi minimal 8 karakter" else null
        val confirmError = if (s.confirmPassword != s.password) "Konfirmasi kata sandi tidak sama" else null
        val birthDateError = AgeCalculator.validate(s.birthDate)
        val hpht = s.hpht
        val hphtError = when {
            hpht == null -> "Pilih tanggal HPHT"
            hpht.isAfter(LocalDate.now()) -> "Tanggal HPHT tidak boleh di masa depan"
            PregnancyCalculator.weeksSince(hpht) > PregnancyCalculator.MAX_WEEKS ->
                "HPHT lebih dari ${PregnancyCalculator.MAX_WEEKS} minggu lalu"
            else -> null
        }

        val hasFieldError = listOf(nameError, emailError, passwordError, confirmError, birthDateError, hphtError)
            .any { it != null }

        if (hasFieldError || !s.agreed) {
            _uiState.update {
                it.copy(
                    nameError = nameError,
                    emailError = emailError,
                    passwordError = passwordError,
                    confirmError = confirmError,
                    birthDateError = birthDateError,
                    hphtError = hphtError,
                    errorMessage = if (!s.agreed) {
                        "Setujui Syarat Penggunaan dan Kebijakan Privasi terlebih dahulu."
                    } else null
                )
            }
            return
        }

        val account = UserAccount(
            name = s.name.trim(),
            email = s.email.trim(),
            birthDate = s.birthDate?.toString().orEmpty(),
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

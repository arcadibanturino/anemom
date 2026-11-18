package com.kelompok.anemom.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kelompok.anemom.data.remote.ApiException
import com.kelompok.anemom.data.repository.AuthRepository
import com.kelompok.anemom.util.AgeCalculator
import com.kelompok.anemom.util.PregnancyCalculator
import com.kelompok.anemom.util.Validators
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

data class EditProfileUiState(
    /** False selama salinan profil belum dibaca; form belum ditampilkan. */
    val isLoaded: Boolean = false,
    val name: String = "",
    val email: String = "",
    val birthDate: LocalDate? = null,
    val hpht: LocalDate? = null,
    val healthFacility: String = "",
    val nameError: String? = null,
    val emailError: String? = null,
    val birthDateError: String? = null,
    val hphtError: String? = null,
    val errorMessage: String? = null,
    val isSaving: Boolean = false
) {
    /** Usia (tahun) dihitung dari tanggal lahir. */
    val ageYears: Int?
        get() = birthDate?.let { AgeCalculator.ageYears(it) }?.takeIf { it >= 0 }

    /** Usia kehamilan (minggu) dihitung dari HPHT. */
    val pregnancyWeeks: Int?
        get() = hpht?.let { PregnancyCalculator.weeksSince(it) }?.takeIf { it >= 0 }

    val trimester: Int?
        get() = pregnancyWeeks?.let { PregnancyCalculator.trimesterFor(it) }

    val dueDate: LocalDate?
        get() = hpht?.let { PregnancyCalculator.dueDate(it) }
}

class EditProfileViewModel(private val authRepository: AuthRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(EditProfileUiState())
    val uiState: StateFlow<EditProfileUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val account = authRepository.account.first()
            _uiState.update {
                it.copy(
                    isLoaded = true,
                    name = account?.name.orEmpty(),
                    email = account?.email.orEmpty(),
                    birthDate = account?.birthDate.toDateOrNull(),
                    hpht = account?.hpht.toDateOrNull(),
                    healthFacility = account?.healthFacility.orEmpty()
                )
            }
        }
    }

    fun onNameChange(v: String) = _uiState.update { it.copy(name = v, nameError = null, errorMessage = null) }
    fun onEmailChange(v: String) = _uiState.update { it.copy(email = v, emailError = null, errorMessage = null) }
    fun onFacilityChange(v: String) = _uiState.update { it.copy(healthFacility = v, errorMessage = null) }

    fun onBirthDateSelected(date: LocalDate) =
        _uiState.update { it.copy(birthDate = date, birthDateError = null, errorMessage = null) }

    fun onHphtSelected(date: LocalDate) =
        _uiState.update { it.copy(hpht = date, hphtError = null, errorMessage = null) }

    fun save(onSaved: () -> Unit) {
        val s = _uiState.value
        if (s.isSaving) return

        val nameError = when {
            s.name.isBlank() -> "Nama atau nama panggilan wajib diisi"
            s.name.trim().length < 2 -> "Nama minimal 2 karakter"
            else -> null
        }
        val emailError = if (!Validators.isValidEmail(s.email)) "Format email tidak valid" else null
        val birthDateError = AgeCalculator.validate(s.birthDate)
        val hpht = s.hpht
        val hphtError = when {
            hpht == null -> "Pilih tanggal HPHT"
            hpht.isAfter(LocalDate.now()) -> "Tanggal HPHT tidak boleh di masa depan"
            PregnancyCalculator.weeksSince(hpht) > PregnancyCalculator.MAX_WEEKS ->
                "HPHT lebih dari ${PregnancyCalculator.MAX_WEEKS} minggu lalu"
            else -> null
        }

        if (listOf(nameError, emailError, birthDateError, hphtError).any { it != null }) {
            _uiState.update {
                it.copy(
                    nameError = nameError,
                    emailError = emailError,
                    birthDateError = birthDateError,
                    hphtError = hphtError
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            authRepository.updateProfile(
                name = s.name,
                email = s.email,
                birthDate = s.birthDate?.toString().orEmpty(),
                hpht = hpht?.toString().orEmpty(),
                healthFacility = s.healthFacility
            )
                .onSuccess {
                    _uiState.update { it.copy(isSaving = false) }
                    onSaved()
                }
                .onFailure { error ->
                    val emailTaken = (error as? ApiException)?.code == "email_taken"
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            emailError = if (emailTaken) error.message else null,
                            errorMessage = if (emailTaken) null else error.message ?: "Gagal menyimpan perubahan. Coba lagi."
                        )
                    }
                }
        }
    }
}

private fun String?.toDateOrNull(): LocalDate? =
    this?.takeIf { it.isNotBlank() }?.let { runCatching { LocalDate.parse(it) }.getOrNull() }

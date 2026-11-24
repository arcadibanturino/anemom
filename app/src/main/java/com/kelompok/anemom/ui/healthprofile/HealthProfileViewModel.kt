package com.kelompok.anemom.ui.healthprofile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kelompok.anemom.data.model.AllergenOption
import com.kelompok.anemom.data.model.AllergyStatus
import com.kelompok.anemom.data.model.AnemiaHistory
import com.kelompok.anemom.data.model.HealthProfileInput
import com.kelompok.anemom.data.repository.AuthRepository
import com.kelompok.anemom.data.repository.NutritionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Daftar bahan untuk checklist alergi: dimuat dari server (GET /ingredients). */
sealed interface AllergenOptionsState {
    data object Loading : AllergenOptionsState
    data class Ready(val items: List<AllergenOption>) : AllergenOptionsState
    data class Failed(val message: String) : AllergenOptionsState
}

data class HealthProfileUiState(
    val height: String = "",
    val weight: String = "",
    val lila: String = "",
    val anemia: AnemiaHistory? = null,
    val allergy: AllergyStatus? = null,
    /** Tetap tersimpan saat Bunda berpindah ke "Tidak"/"Tidak Tahu", tetapi hanya dikirim bila status = Ada. */
    val selectedAllergens: Set<Long> = emptySet(),
    val allergenSearch: String = "",
    val options: AllergenOptionsState = AllergenOptionsState.Loading,
    val heightError: String? = null,
    val weightError: String? = null,
    val anemiaError: String? = null,
    val allergyError: String? = null,
    val allergenError: String? = null,
    val lilaError: String? = null,
    val errorMessage: String? = null,
    val isSaving: Boolean = false
)

/**
 * Langkah 2 pendaftaran. Akun sudah dibuat di langkah 1, jadi keluar dari layar ini berarti logout;
 * saat masuk lagi, Bunda diarahkan kembali ke sini selama profile_completed = false.
 */
class HealthProfileViewModel(
    private val authRepository: AuthRepository,
    private val nutritionRepository: NutritionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HealthProfileUiState())
    val uiState: StateFlow<HealthProfileUiState> = _uiState.asStateFlow()

    init {
        loadOptions()
        prefill()
    }

    fun onHeightChange(v: String) =
        _uiState.update { it.copy(height = v.decimalInput(), heightError = null, errorMessage = null) }

    fun onWeightChange(v: String) =
        _uiState.update { it.copy(weight = v.decimalInput(), weightError = null, errorMessage = null) }

    fun onLilaChange(v: String) =
        _uiState.update { it.copy(lila = v.decimalInput(), lilaError = null, errorMessage = null) }

    fun onAnemiaChange(v: AnemiaHistory) =
        _uiState.update { it.copy(anemia = v, anemiaError = null, errorMessage = null) }

    fun onAllergyChange(v: AllergyStatus) =
        _uiState.update { it.copy(allergy = v, allergyError = null, allergenError = null, errorMessage = null) }

    fun onAllergenSearchChange(v: String) = _uiState.update { it.copy(allergenSearch = v) }

    fun onToggleAllergen(id: Long) = _uiState.update {
        val next = if (id in it.selectedAllergens) it.selectedAllergens - id else it.selectedAllergens + id
        it.copy(selectedAllergens = next, allergenError = null, errorMessage = null)
    }

    fun loadOptions() {
        _uiState.update { it.copy(options = AllergenOptionsState.Loading) }
        viewModelScope.launch {
            nutritionRepository.allergenOptions()
                .onSuccess { list ->
                    val sorted = list.sortedBy { it.name.lowercase() }
                    _uiState.update { it.copy(options = AllergenOptionsState.Ready(sorted)) }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(options = AllergenOptionsState.Failed(e.message ?: "Gagal memuat daftar bahan."))
                    }
                }
        }
    }

    /** Lanjutkan pengisian: isi dari server (mis. tinggi/berat yang sudah ada), tanpa menimpa ketikan Bunda. */
    private fun prefill() {
        viewModelScope.launch {
            authRepository.loadHealthProfile().onSuccess { p ->
                _uiState.update { s ->
                    s.copy(
                        height = s.height.ifEmpty { p.heightCm?.formatInput().orEmpty() },
                        weight = s.weight.ifEmpty { p.preWeightKg?.formatInput().orEmpty() },
                        lila = s.lila.ifEmpty { p.lilaCm?.formatInput().orEmpty() },
                        anemia = s.anemia ?: p.anemiaHistory,
                        allergy = s.allergy ?: p.allergyStatus,
                        selectedAllergens = s.selectedAllergens.ifEmpty { p.allergenIds }
                    )
                }
            }
            // Gagal memuat tidak perlu pesan: formulir kosong tetap bisa diisi dan disimpan.
        }
    }

    fun save(onSuccess: () -> Unit) {
        val s = _uiState.value
        if (s.isSaving) return

        val height = s.height.toNumber()
        val weight = s.weight.toNumber()
        val lila = s.lila.toNumber()

        // Id yang sudah tidak ada di katalog tidak ikut dikirim (server menolak id tak dikenal).
        val knownIds = (s.options as? AllergenOptionsState.Ready)?.items?.map { it.id }?.toSet()
        val selected = if (knownIds != null) s.selectedAllergens.intersect(knownIds) else s.selectedAllergens

        val heightError = rangeError(height, HEIGHT_MIN, HEIGHT_MAX) { "Tinggi badan harus $it cm." }
        val weightError = rangeError(weight, WEIGHT_MIN, WEIGHT_MAX) { "Berat badan harus $it kg." }
        val lilaError = when {
            s.lila.isBlank() -> null   // opsional
            lila == null -> "Isi angka yang valid."
            lila < LILA_MIN || lila > LILA_MAX -> "LiLA harus ${LILA_MIN.toInt()}-${LILA_MAX.toInt()} cm."
            else -> null
        }
        val anemiaError = if (s.anemia == null) "Pilih Ya, Tidak, atau Tidak Tahu." else null
        val allergyError = if (s.allergy == null) "Pilih status alergi. Jika Ada, detail wajib diisi." else null
        val allergenError = if (s.allergy == AllergyStatus.ADA && selected.isEmpty()) {
            "Pilih minimal satu bahan yang menyebabkan alergi."
        } else null

        val hasError = listOf(heightError, weightError, lilaError, anemiaError, allergyError, allergenError)
            .any { it != null }
        if (hasError) {
            _uiState.update {
                it.copy(
                    heightError = heightError,
                    weightError = weightError,
                    lilaError = lilaError,
                    anemiaError = anemiaError,
                    allergyError = allergyError,
                    allergenError = allergenError,
                    errorMessage = null
                )
            }
            return
        }

        val input = HealthProfileInput(
            heightCm = height ?: return,
            preWeightKg = weight ?: return,
            anemiaHistory = s.anemia ?: return,
            allergyStatus = s.allergy ?: return,
            lilaCm = if (s.lila.isBlank()) null else lila,
            allergenIds = selected
        )

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            authRepository.saveHealthProfile(input)
                .onSuccess {
                    _uiState.update { it.copy(isSaving = false) }
                    onSuccess()
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(isSaving = false, errorMessage = error.message ?: "Gagal menyimpan profil. Coba lagi.")
                    }
                }
        }
    }

    /** Keluar dari pendaftaran. Akun tetap ada; profil dilengkapi saat Bunda masuk kembali. */
    fun logout(onDone: () -> Unit) {
        viewModelScope.launch {
            authRepository.logout()
            onDone()
        }
    }

    private companion object {
        const val HEIGHT_MIN = 100.0
        const val HEIGHT_MAX = 220.0
        const val WEIGHT_MIN = 30.0
        const val WEIGHT_MAX = 200.0
        const val LILA_MIN = 15.0
        const val LILA_MAX = 50.0
    }
}

/** Angka > 0 dan dalam rentang; batas sama dengan server (PUT /me/health-profile). */
private fun rangeError(value: Double?, min: Double, max: Double, outOfRange: (String) -> String): String? = when {
    value == null || value <= 0.0 -> "Isi angka lebih dari 0."
    value < min || value > max -> outOfRange("${min.toInt()}-${max.toInt()}")
    else -> null
}

/** Terima koma maupun titik sebagai pemisah desimal ("52,5" atau "52.5"). */
private fun String.toNumber(): Double? = trim().replace(',', '.').toDoubleOrNull()

/** Hanya angka dan satu pemisah desimal, maksimal 5 karakter. */
private fun String.decimalInput(): String {
    var hasSeparator = false
    val out = StringBuilder()
    for (c in this) {
        when {
            c in '0'..'9' -> out.append(c)
            (c == '.' || c == ',') && !hasSeparator -> {
                hasSeparator = true
                out.append(c)
            }
        }
    }
    return out.toString().take(5)
}

/** 158.0 -> "158", 52.5 -> "52.5". */
private fun Double.formatInput(): String = if (this % 1.0 == 0.0) toInt().toString() else toString()

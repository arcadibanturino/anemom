package com.kelompok.anemom.ui.nutrisi

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kelompok.anemom.data.model.FoodDetail
import com.kelompok.anemom.data.repository.NutritionRepository
import com.kelompok.anemom.ui.navigation.Routes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class FoodDetailUiState(
    val isLoading: Boolean = true,
    val detail: FoodDetail? = null,
    val error: String? = null
)

/** Detail Bahan Makanan. ID bahan dari argumen navigasi [Routes.FOOD_DETAIL_ARG_ID]. */
class FoodDetailViewModel(
    savedStateHandle: SavedStateHandle,
    private val repository: NutritionRepository
) : ViewModel() {

    private val foodId: Long = savedStateHandle.get<Long>(Routes.FOOD_DETAIL_ARG_ID) ?: 0L

    private val _uiState = MutableStateFlow(FoodDetailUiState())
    val uiState: StateFlow<FoodDetailUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        _uiState.value = FoodDetailUiState(isLoading = true)
        viewModelScope.launch {
            repository.ingredientDetail(foodId)
                .onSuccess { _uiState.value = FoodDetailUiState(isLoading = false, detail = it) }
                .onFailure { e ->
                    _uiState.value = FoodDetailUiState(
                        isLoading = false,
                        error = e.message ?: "Detail bahan gagal dimuat."
                    )
                }
        }
    }
}

package com.kelompok.anemom.ui.nutrisi

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kelompok.anemom.data.model.FoodContext
import com.kelompok.anemom.data.model.FoodGroup
import com.kelompok.anemom.data.model.FoodItem
import com.kelompok.anemom.data.model.FoodRecommendations
import com.kelompok.anemom.data.repository.NutritionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn

/** Tab yang tampil di layar, sesuai urutan desain. */
val MENU_TABS = listOf(FoodGroup.SAYUR, FoodGroup.BUAH, FoodGroup.PROTEIN)

data class MenuListUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val context: FoodContext = FoodContext.EMPTY,
    val selectedGroup: FoodGroup = FoodGroup.SAYUR,
    val itemsByGroup: Map<FoodGroup, List<FoodItem>> = emptyMap()
) {
    val visibleItems: List<FoodItem> get() = itemsByGroup[selectedGroup].orEmpty()
}

/** Layar Rekomendasi Makanan: bahan dari server dikelompokkan per tab Sayur / Buah / Protein. */
class MenuListViewModel(private val repository: NutritionRepository) : ViewModel() {

    private val selectedGroup = MutableStateFlow(FoodGroup.SAYUR)

    private val data = repository.recommendations
        .map<FoodRecommendations, FoodRecommendations?> { it }
        .onStart { emit(null) }

    val uiState: StateFlow<MenuListUiState> =
        combine(data, repository.recommendationsError, selectedGroup) { rec, error, group ->
            MenuListUiState(
                isLoading = rec == null && error == null,
                error = if (rec == null) error else null,
                context = rec?.context ?: FoodContext.EMPTY,
                selectedGroup = group,
                // Urutan dari server (recommend_order) dipertahankan di tiap kelompok.
                itemsByGroup = rec?.items.orEmpty().groupBy { it.group }
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = MenuListUiState()
        )

    fun selectGroup(group: FoodGroup) {
        selectedGroup.value = group
    }

    fun retry() = repository.refresh()
}

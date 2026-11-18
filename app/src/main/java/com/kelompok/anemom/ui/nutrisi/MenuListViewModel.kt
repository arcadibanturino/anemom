package com.kelompok.anemom.ui.nutrisi

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kelompok.anemom.data.repository.NutritionRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class MenuListViewModel(nutritionRepository: NutritionRepository) : ViewModel() {

    /** Daftar dari server; kosong sebelum data datang atau jika gagal dimuat. */
    val menus: StateFlow<List<MenuRecommendation>> = nutritionRepository.foods
        .map { it.toMenuRecommendations() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )
}

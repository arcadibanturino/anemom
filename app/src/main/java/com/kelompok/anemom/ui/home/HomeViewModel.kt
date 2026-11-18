package com.kelompok.anemom.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kelompok.anemom.data.model.DashboardData
import com.kelompok.anemom.data.repository.HomeRepository
import com.kelompok.anemom.data.repository.NutritionRepository
import com.kelompok.anemom.ui.nutrisi.DailyMenuPlanner
import com.kelompok.anemom.ui.nutrisi.MenuRecommendation
import com.kelompok.anemom.ui.nutrisi.toMenuRecommendations
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class HomeViewModel(
    private val homeRepository: HomeRepository,
    nutritionRepository: NutritionRepository
) : ViewModel() {

    /** Null selama data pertama dari server belum datang. */
    val dashboard: StateFlow<DashboardData?> = homeRepository.dashboard()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null
        )

    /** Pesan galat terakhir saat memuat Beranda; null jika berhasil. */
    val loadError: StateFlow<String?> = homeRepository.loadError

    /**
     * Saran bahan hari ini: dipilih dari rekomendasi server oleh [DailyMenuPlanner] (berganti tiap hari,
     * menyesuaikan Hb terakhir). Kosong sebelum data datang atau jika gagal dimuat.
     */
    val menus: StateFlow<List<MenuRecommendation>> = combine(nutritionRepository.foods, dashboard) { foods, dash ->
        DailyMenuPlanner.plan(foods, dash?.risk).toMenuRecommendations()
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    fun retry() = homeRepository.retry()
}

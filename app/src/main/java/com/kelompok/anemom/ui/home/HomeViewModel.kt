package com.kelompok.anemom.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kelompok.anemom.data.model.DashboardData
import com.kelompok.anemom.data.repository.HomeRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class HomeViewModel(homeRepository: HomeRepository) : ViewModel() {

    val dashboard: StateFlow<DashboardData?> = homeRepository.dashboard()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null
        )
}

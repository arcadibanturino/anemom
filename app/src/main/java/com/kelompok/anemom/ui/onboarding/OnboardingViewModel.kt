package com.kelompok.anemom.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kelompok.anemom.data.repository.AuthRepository
import kotlinx.coroutines.launch

class OnboardingViewModel(private val authRepository: AuthRepository) : ViewModel() {

    /** Menandai onboarding selesai supaya tidak tampil lagi saat aplikasi dibuka berikutnya. */
    fun finishOnboarding(onDone: () -> Unit) {
        viewModelScope.launch {
            authRepository.setOnboardingDone()
            onDone()
        }
    }
}

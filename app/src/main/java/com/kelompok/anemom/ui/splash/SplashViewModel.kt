package com.kelompok.anemom.ui.splash

import androidx.lifecycle.ViewModel
import com.kelompok.anemom.data.repository.AuthRepository
import com.kelompok.anemom.ui.navigation.Routes
import kotlinx.coroutines.flow.first

class SplashViewModel(private val authRepository: AuthRepository) : ViewModel() {

    /** Menentukan halaman pertama setelah splash: onboarding -> login -> beranda. */
    suspend fun resolveStartRoute(): String {
        if (!authRepository.onboardingDone.first()) return Routes.ONBOARDING
        return if (authRepository.isLoggedIn.first()) Routes.MAIN else Routes.LOGIN
    }
}

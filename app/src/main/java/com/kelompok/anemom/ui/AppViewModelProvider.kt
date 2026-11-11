package com.kelompok.anemom.ui

import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.kelompok.anemom.AnemomApp
import com.kelompok.anemom.ui.auth.LoginViewModel
import com.kelompok.anemom.ui.auth.RegisterViewModel
import com.kelompok.anemom.ui.home.HomeViewModel
import com.kelompok.anemom.ui.onboarding.OnboardingViewModel
import com.kelompok.anemom.ui.profile.ProfileViewModel
import com.kelompok.anemom.ui.riwayat.RiwayatViewModel
import com.kelompok.anemom.ui.skrining.InputHbViewModel
import com.kelompok.anemom.ui.skrining.SmartwatchViewModel
import com.kelompok.anemom.ui.splash.SplashViewModel
import com.kelompok.anemom.ui.ttd.TtdViewModel

/** Satu factory untuk semua ViewModel; dependensi diambil dari AppContainer. */
object AppViewModelProvider {
    val Factory = viewModelFactory {
        initializer { SplashViewModel(anemomApp().container.authRepository) }
        initializer { OnboardingViewModel(anemomApp().container.authRepository) }
        initializer { LoginViewModel(anemomApp().container.authRepository) }
        initializer { RegisterViewModel(anemomApp().container.authRepository) }
        initializer { HomeViewModel(anemomApp().container.homeRepository) }
        initializer { ProfileViewModel(anemomApp().container.authRepository) }
        initializer { TtdViewModel(anemomApp().container.ttdRepository) }
        initializer { RiwayatViewModel(anemomApp().container.hbRepository) }
        initializer {
            InputHbViewModel(anemomApp().container.hbRepository, anemomApp().container.authRepository)
        }
        initializer { SmartwatchViewModel() }
    }
}

private fun CreationExtras.anemomApp(): AnemomApp =
    this[APPLICATION_KEY] as AnemomApp

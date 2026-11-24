package com.kelompok.anemom.ui

import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.kelompok.anemom.AnemomApp
import com.kelompok.anemom.ui.auth.LoginViewModel
import com.kelompok.anemom.ui.auth.RegisterViewModel
import com.kelompok.anemom.ui.healthprofile.HealthProfileViewModel
import com.kelompok.anemom.ui.home.HomeViewModel
import com.kelompok.anemom.ui.notifikasi.NotificationsViewModel
import com.kelompok.anemom.ui.pengingat.ReminderViewModel
import com.kelompok.anemom.ui.onboarding.OnboardingViewModel
import com.kelompok.anemom.ui.profile.EditProfileViewModel
import com.kelompok.anemom.ui.profile.ProfileViewModel
import com.kelompok.anemom.ui.riwayat.RiwayatViewModel
import com.kelompok.anemom.ui.riwayat.HbChartViewModel
import com.kelompok.anemom.ui.riwayat.HistoryDetailViewModel
import com.kelompok.anemom.ui.privasi.PrivacyViewModel
import com.kelompok.anemom.ui.skrining.InputHbViewModel
import com.kelompok.anemom.ui.skrining.smartband.PpgRecordViewModel
import com.kelompok.anemom.ui.skrining.smartband.SmartbandConnectViewModel
import com.kelompok.anemom.ui.splash.SplashViewModel
import com.kelompok.anemom.ui.ttd.TtdViewModel
import com.kelompok.anemom.ui.auth.ForgotPasswordViewModel
import com.kelompok.anemom.ui.nutrisi.FoodDetailViewModel
import com.kelompok.anemom.ui.nutrisi.MenuListViewModel
import com.kelompok.anemom.ui.skrining.scan.AiScanViewModel

/** Satu factory untuk semua ViewModel; dependensi diambil dari AppContainer. */
object AppViewModelProvider {
    val Factory = viewModelFactory {
        initializer { SplashViewModel(anemomApp().container.authRepository) }
        initializer { OnboardingViewModel(anemomApp().container.authRepository) }
        initializer { LoginViewModel(anemomApp().container.authRepository) }
        initializer { RegisterViewModel(anemomApp().container.authRepository) }
        initializer {
            HealthProfileViewModel(
                anemomApp().container.authRepository,
                anemomApp().container.nutritionRepository
            )
        }
        initializer { ForgotPasswordViewModel() }
        initializer {
            HomeViewModel(
                anemomApp().container.homeRepository,
                anemomApp().container.nutritionRepository,
                anemomApp().container.notificationRepository
            )
        }
        initializer { NotificationsViewModel(anemomApp().container.notificationRepository) }
        initializer { MenuListViewModel(anemomApp().container.nutritionRepository) }
        initializer {
            FoodDetailViewModel(createSavedStateHandle(), anemomApp().container.nutritionRepository)
        }
        initializer { ProfileViewModel(anemomApp().container.authRepository) }
        initializer { EditProfileViewModel(anemomApp().container.authRepository) }
        initializer { TtdViewModel(anemomApp().container.ttdRepository) }
        initializer { ReminderViewModel(anemomApp().container.ttdRepository, anemomApp().container.ttdAlarm) }
        initializer { RiwayatViewModel(anemomApp().container.hbRepository, anemomApp().container.screeningRepository) }
        initializer {
            HistoryDetailViewModel(
                createSavedStateHandle(),
                anemomApp().container.hbRepository,
                anemomApp().container.screeningRepository
            )
        }
        initializer { HbChartViewModel(anemomApp().container.hbRepository) }
        initializer { PrivacyViewModel(anemomApp().container.consentRepository) }
        initializer {
            InputHbViewModel(anemomApp().container.hbRepository, anemomApp().container.authRepository)
        }
        initializer { SmartbandConnectViewModel(anemomApp().container.smartbandRepository) }
        initializer { PpgRecordViewModel(anemomApp().container.smartbandRepository) }
        initializer {
            val app = anemomApp()
            // Model segmentasi dimuat saat foto pertama diperiksa, bukan saat layar dibuka.
            AiScanViewModel({ app.container.segmenter }, app.container.screeningRepository, app.cacheDir)
        }
    }
}

private fun CreationExtras.anemomApp(): AnemomApp =
    this[APPLICATION_KEY] as AnemomApp

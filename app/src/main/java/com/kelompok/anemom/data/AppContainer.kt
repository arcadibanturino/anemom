package com.kelompok.anemom.data

import android.content.Context
import com.kelompok.anemom.BuildConfig
import com.kelompok.anemom.data.local.SessionManager
import com.kelompok.anemom.data.remote.ApiService
import com.kelompok.anemom.data.remote.RetrofitClient
import com.kelompok.anemom.data.repository.AuthRepository
import com.kelompok.anemom.data.repository.HbRepository
import com.kelompok.anemom.data.repository.HomeRepository
import com.kelompok.anemom.data.repository.NutritionRepository
import com.kelompok.anemom.data.repository.ScreeningRepository
import com.kelompok.anemom.data.repository.TtdRepository

/** Wadah dependensi: satu tempat untuk membuat SessionManager, ApiService, dan Repository. */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    /** Saluran bersama: penanda data berubah + pesan singkat untuk Toast. */
    val events = AppEvents()

    val sessionManager: SessionManager by lazy { SessionManager(appContext) }

    val apiService: ApiService by lazy { RetrofitClient.create(BuildConfig.BASE_URL, sessionManager) }

    val authRepository: AuthRepository by lazy { AuthRepository(sessionManager, apiService, events) }

    val hbRepository: HbRepository by lazy { HbRepository(apiService, events) }

    val ttdRepository: TtdRepository by lazy { TtdRepository(apiService, events) }

    val homeRepository: HomeRepository by lazy { HomeRepository(apiService, events) }

    val nutritionRepository: NutritionRepository by lazy { NutritionRepository(apiService, events) }

    val screeningRepository: ScreeningRepository by lazy { ScreeningRepository(apiService, events) }
}

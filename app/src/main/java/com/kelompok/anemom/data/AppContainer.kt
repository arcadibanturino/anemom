package com.kelompok.anemom.data

import android.content.Context
import com.kelompok.anemom.BuildConfig
import com.kelompok.anemom.data.local.HealthStore
import com.kelompok.anemom.data.local.SessionManager
import com.kelompok.anemom.data.remote.ApiService
import com.kelompok.anemom.data.remote.RetrofitClient
import com.kelompok.anemom.data.repository.AuthRepository
import com.kelompok.anemom.data.repository.HbRepository
import com.kelompok.anemom.data.repository.HomeRepository
import com.kelompok.anemom.data.repository.TtdRepository

/** Wadah dependensi: satu tempat untuk membuat SessionManager, ApiService, dan Repository. */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val sessionManager: SessionManager by lazy { SessionManager(appContext) }

    private val healthStore: HealthStore by lazy { HealthStore(appContext) }

    val apiService: ApiService by lazy { RetrofitClient.create(BuildConfig.BASE_URL) }

    val authRepository: AuthRepository by lazy { AuthRepository(sessionManager, apiService) }

    val hbRepository: HbRepository by lazy { HbRepository(healthStore) }

    val ttdRepository: TtdRepository by lazy { TtdRepository(healthStore) }

    val homeRepository: HomeRepository by lazy {
        HomeRepository(sessionManager, hbRepository, ttdRepository)
    }
}

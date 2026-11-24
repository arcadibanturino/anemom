package com.kelompok.anemom.data

import android.content.Context
import com.kelompok.anemom.BuildConfig
import com.kelompok.anemom.alarm.TtdAlarmScheduler
import com.kelompok.anemom.data.local.SessionManager
import com.kelompok.anemom.data.remote.ApiService
import com.kelompok.anemom.data.remote.RetrofitClient
import com.kelompok.anemom.data.repository.AuthRepository
import com.kelompok.anemom.data.repository.ConsentRepository
import com.kelompok.anemom.data.repository.HbRepository
import com.kelompok.anemom.data.repository.HomeRepository
import com.kelompok.anemom.data.repository.NotificationRepository
import com.kelompok.anemom.data.repository.NutritionRepository
import com.kelompok.anemom.data.repository.ScreeningRepository
import com.kelompok.anemom.data.repository.TtdRepository
import com.kelompok.anemom.data.smartband.SimulatedSmartbandRepository
import com.kelompok.anemom.data.smartband.SmartbandRepository
import com.kelompok.anemom.ml.segmentation.ConjunctivaSegmenter
import com.kelompok.anemom.ml.segmentation.SegmenterProvider

/** Wadah dependensi: satu tempat untuk membuat SessionManager, ApiService, dan Repository. */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    /** Saluran bersama: penanda data berubah + pesan singkat untuk Toast. */
    val events = AppEvents()

    val sessionManager: SessionManager by lazy { SessionManager(appContext) }

    val apiService: ApiService by lazy { RetrofitClient.create(BuildConfig.BASE_URL, sessionManager) }

    val authRepository: AuthRepository by lazy { AuthRepository(sessionManager, apiService, events) }

    val hbRepository: HbRepository by lazy { HbRepository(apiService, events) }

    /** Alarm TTD harian di HP (AlarmManager). */
    val ttdAlarm: TtdAlarmScheduler by lazy { TtdAlarmScheduler(appContext) }

    val ttdRepository: TtdRepository by lazy { TtdRepository(apiService, events, ttdAlarm) }

    val homeRepository: HomeRepository by lazy { HomeRepository(apiService, events) }

    val nutritionRepository: NutritionRepository by lazy { NutritionRepository(apiService, events) }

    val screeningRepository: ScreeningRepository by lazy { ScreeningRepository(apiService, events) }

    /** Persetujuan Privasi & disclaimer. */
    val consentRepository: ConsentRepository by lazy { ConsentRepository(apiService) }

    /** Notifikasi + jumlah belum dibaca, dipakai bersama Beranda dan layar Notifikasi. */
    val notificationRepository: NotificationRepository by lazy { NotificationRepository(apiService) }

    /**
     * Smartband (Bluetooth + sinyal PPG). Satu instance dipakai bersama layar Koneksi Bluetooth
     * dan Rekam PPG, supaya status sambungan sama di keduanya.
     * TODO(ble): ganti dengan BleSmartbandRepository(appContext, apiService) saat perangkat IoT siap.
     */
    val smartbandRepository: SmartbandRepository by lazy { SimulatedSmartbandRepository() }

    /**
     * Model segmentasi konjungtiva. U-Net atau YOLO ditentukan flavor saat build (unetDebug / yoloDebug);
     * dimuat saat pertama kali dipakai layar AI Scan Mata.
     */
    val segmenter: ConjunctivaSegmenter by lazy { SegmenterProvider.create(appContext) }
}

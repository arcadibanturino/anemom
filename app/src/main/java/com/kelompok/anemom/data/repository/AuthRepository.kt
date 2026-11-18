package com.kelompok.anemom.data.repository

import android.os.Build
import com.kelompok.anemom.data.AppEvents
import com.kelompok.anemom.data.local.SessionManager
import com.kelompok.anemom.data.local.UserAccount
import com.kelompok.anemom.data.remote.ApiService
import com.kelompok.anemom.data.remote.apiCall
import com.kelompok.anemom.data.remote.dto.LoginRequest
import com.kelompok.anemom.data.remote.dto.RegisterRequest
import com.kelompok.anemom.data.remote.dto.UpdateProfileRequest
import com.kelompok.anemom.data.remote.dto.UserDto
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch

/**
 * Autentikasi lewat API backend. Server adalah sumber kebenaran: kata sandi tidak pernah
 * disimpan di HP, yang disimpan hanya token sesi dan salinan profil (lihat [SessionManager]).
 */
class AuthRepository(
    private val session: SessionManager,
    private val api: ApiService,
    private val events: AppEvents
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var lastProfileRefresh = 0L

    val onboardingDone: Flow<Boolean> = session.onboardingDone
    val isLoggedIn: Flow<Boolean> = session.isLoggedIn

    /** Profil dari salinan lokal; setiap ada yang berlangganan, profil disegarkan dari server di latar belakang. */
    val account: Flow<UserAccount?> = session.account.onStart {
        scope.launch { refreshProfile() }
    }

    suspend fun setOnboardingDone() = session.setOnboardingDone()

    /**
     * Daftar akun baru. Dari [account] hanya name, email, birthDate, dan hpht yang dikirim;
     * usia kehamilan dan trimester dihitung server dari HPHT.
     */
    suspend fun register(account: UserAccount, password: String): Result<Unit> =
        apiCall {
            api.register(
                RegisterRequest(
                    name = account.name.trim(),
                    email = account.email.trim(),
                    password = password,
                    birthDate = account.birthDate,
                    hpht = account.hpht,
                    deviceName = deviceName()
                )
            )
        }.mapCatching { data ->
            session.saveSession(data.token, data.user.toAccount())
            events.invalidateData()
        }

    suspend fun login(email: String, password: String): Result<Unit> =
        apiCall {
            api.login(LoginRequest(email = email.trim(), password = password, deviceName = deviceName()))
        }.mapCatching { data ->
            session.saveSession(data.token, data.user.toAccount())
            events.invalidateData()
        }

    /** Keluar. Token dicabut di server (jika ada koneksi), lalu sesi lokal dihapus apa pun hasilnya. */
    suspend fun logout() {
        runCatching { api.logout() }
        session.clearSession()
    }

    /** Segarkan salinan profil dari GET /me (dibatasi sekali per 30 detik). */
    suspend fun refreshProfile(): Result<Unit> {
        if (session.token.first() == null) return Result.success(Unit)
        val now = System.currentTimeMillis()
        if (now - lastProfileRefresh < 30_000) return Result.success(Unit)
        lastProfileRefresh = now
        return apiCall { api.me() }.mapCatching { session.saveAccount(it.toAccount()) }
    }

    /**
     * Simpan perubahan profil (PUT /me). Berhasil = salinan lokal diganti dengan data dari server
     * dan semua layar mengambil ulang datanya (usia kehamilan di Beranda ikut berubah bila HPHT diubah).
     */
    suspend fun updateProfile(
        name: String,
        email: String,
        birthDate: String,
        hpht: String,
        healthFacility: String
    ): Result<Unit> =
        apiCall {
            api.updateMe(
                UpdateProfileRequest(
                    name = name.trim(),
                    email = email.trim(),
                    birthDate = birthDate,
                    hpht = hpht,
                    healthFacility = healthFacility.trim()
                )
            )
        }.mapCatching { user ->
            session.saveAccount(user.toAccount())
            lastProfileRefresh = System.currentTimeMillis()
            events.invalidateData()
        }

    private fun deviceName(): String = "${Build.MANUFACTURER} ${Build.MODEL}".take(100)
}

private fun UserDto.toAccount() = UserAccount(
    name = name,
    email = email,
    birthDate = profile.birthDate.orEmpty(),
    hpht = profile.hpht.orEmpty(),
    trimester = pregnancy.trimester,
    healthFacility = profile.healthFacility.orEmpty()
)

package com.kelompok.anemom.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.anemomDataStore: DataStore<Preferences> by preferencesDataStore(name = "anemom_prefs")

/**
 * Salinan profil pengguna di perangkat (sumber aslinya server: GET /me).
 * Dipakai supaya nama/profil langsung tampil sebelum respons server datang.
 * [birthDate] dan [hpht] berformat ISO (yyyy-MM-dd); kosong jika belum diisi.
 * Usia, usia kehamilan, trimester, dan HPL dihitung dari kedua tanggal itu.
 */
data class UserAccount(
    val name: String,
    val email: String,
    val birthDate: String = "",
    val hpht: String,
    val trimester: Int,
    val healthFacility: String = "",
    /** false = langkah 2 pendaftaran (Profil Kesehatan Awal) belum diisi; dashboard belum boleh dibuka. */
    val profileCompleted: Boolean = true
)

/** Menyimpan status onboarding, token sesi, dan salinan profil memakai DataStore. */
class SessionManager(context: Context) {

    private val dataStore = context.anemomDataStore

    private object Keys {
        val ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
        val TOKEN = stringPreferencesKey("auth_token")
        val NAME = stringPreferencesKey("account_name")
        val EMAIL = stringPreferencesKey("account_email")
        val BIRTH_DATE = stringPreferencesKey("account_birth_date")
        val FACILITY = stringPreferencesKey("account_health_facility")
        val HPHT = stringPreferencesKey("account_hpht")
        val TRIMESTER = intPreferencesKey("account_trimester")
        val PROFILE_COMPLETED = booleanPreferencesKey("account_profile_completed")
    }

    private val preferences: Flow<Preferences> = dataStore.data.catch { e ->
        if (e is IOException) emit(emptyPreferences()) else throw e
    }

    val onboardingDone: Flow<Boolean> = preferences.map { it[Keys.ONBOARDING_DONE] ?: false }

    /** Token Bearer; null jika belum masuk. */
    val token: Flow<String?> = preferences
        .map { it[Keys.TOKEN]?.takeIf { token -> token.isNotBlank() } }
        .distinctUntilChanged()

    /** Dianggap sudah masuk selama token tersimpan (token 30 hari; server yang menolak bila kedaluwarsa). */
    val isLoggedIn: Flow<Boolean> = token.map { it != null }

    /**
     * Profil Kesehatan Awal sudah diisi? Kunci yang belum ada (sesi dari versi aplikasi sebelum fitur ini)
     * dianggap sudah; GET /me berikutnya mengoreksinya dari server.
     */
    val profileCompleted: Flow<Boolean> = preferences
        .map { it[Keys.PROFILE_COMPLETED] ?: true }
        .distinctUntilChanged()

    val account: Flow<UserAccount?> = preferences.map { p ->
        val email = p[Keys.EMAIL] ?: return@map null
        UserAccount(
            name = p[Keys.NAME].orEmpty(),
            email = email,
            birthDate = p[Keys.BIRTH_DATE].orEmpty(),
            hpht = p[Keys.HPHT].orEmpty(),
            trimester = p[Keys.TRIMESTER] ?: 1,
            healthFacility = p[Keys.FACILITY].orEmpty(),
            profileCompleted = p[Keys.PROFILE_COMPLETED] ?: true
        )
    }

    suspend fun setOnboardingDone() {
        dataStore.edit { it[Keys.ONBOARDING_DONE] = true }
    }

    /** Simpan token + profil sekaligus setelah register/login berhasil. */
    suspend fun saveSession(token: String, account: UserAccount) {
        dataStore.edit {
            it[Keys.TOKEN] = token
            writeAccount(it, account)
        }
    }

    /** Perbarui salinan profil (mis. setelah GET /me). */
    suspend fun saveAccount(account: UserAccount) {
        dataStore.edit { writeAccount(it, account) }
    }

    /** Keluar: hapus token dan profil. Status onboarding dibiarkan. */
    suspend fun clearSession() {
        dataStore.edit {
            it.remove(Keys.TOKEN)
            it.remove(Keys.NAME)
            it.remove(Keys.EMAIL)
            it.remove(Keys.BIRTH_DATE)
            it.remove(Keys.FACILITY)
            it.remove(Keys.HPHT)
            it.remove(Keys.TRIMESTER)
            it.remove(Keys.PROFILE_COMPLETED)
        }
    }

    private fun writeAccount(p: androidx.datastore.preferences.core.MutablePreferences, account: UserAccount) {
        p[Keys.NAME] = account.name
        p[Keys.EMAIL] = account.email
        p[Keys.BIRTH_DATE] = account.birthDate
        p[Keys.FACILITY] = account.healthFacility
        p[Keys.HPHT] = account.hpht
        p[Keys.TRIMESTER] = account.trimester
        p[Keys.PROFILE_COMPLETED] = account.profileCompleted
    }
}

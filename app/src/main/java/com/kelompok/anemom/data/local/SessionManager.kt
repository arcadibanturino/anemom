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
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.anemomDataStore: DataStore<Preferences> by preferencesDataStore(name = "anemom_prefs")

/**
 * Akun yang tersimpan di perangkat.
 * [hpht] disimpan dalam format ISO (yyyy-MM-dd).
 *
 * TODO(backend): saat backend siap, akun disimpan di server dan di sini
 * cukup menyimpan token sesi.
 */
data class UserAccount(
    val name: String,
    val email: String,
    val phone: String,
    val passwordHash: String,
    val hpht: String,
    val trimester: Int
)

/** Menyimpan status onboarding, status login, dan akun lokal memakai DataStore. */
class SessionManager(context: Context) {

    private val dataStore = context.anemomDataStore

    private object Keys {
        val ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
        val LOGGED_IN = booleanPreferencesKey("logged_in")
        val NAME = stringPreferencesKey("account_name")
        val EMAIL = stringPreferencesKey("account_email")
        val PHONE = stringPreferencesKey("account_phone")
        val PASSWORD_HASH = stringPreferencesKey("account_password_hash")
        val HPHT = stringPreferencesKey("account_hpht")
        val TRIMESTER = intPreferencesKey("account_trimester")
    }

    private val preferences: Flow<Preferences> = dataStore.data.catch { e ->
        if (e is IOException) emit(emptyPreferences()) else throw e
    }

    val onboardingDone: Flow<Boolean> = preferences.map { it[Keys.ONBOARDING_DONE] ?: false }

    val isLoggedIn: Flow<Boolean> = preferences.map { it[Keys.LOGGED_IN] ?: false }

    val account: Flow<UserAccount?> = preferences.map { p ->
        val email = p[Keys.EMAIL] ?: return@map null
        UserAccount(
            name = p[Keys.NAME].orEmpty(),
            email = email,
            phone = p[Keys.PHONE].orEmpty(),
            passwordHash = p[Keys.PASSWORD_HASH].orEmpty(),
            hpht = p[Keys.HPHT].orEmpty(),
            trimester = p[Keys.TRIMESTER] ?: 1
        )
    }

    suspend fun setOnboardingDone() {
        dataStore.edit { it[Keys.ONBOARDING_DONE] = true }
    }

    suspend fun setLoggedIn(value: Boolean) {
        dataStore.edit { it[Keys.LOGGED_IN] = value }
    }

    suspend fun saveAccount(account: UserAccount) {
        dataStore.edit {
            it[Keys.NAME] = account.name
            it[Keys.EMAIL] = account.email
            it[Keys.PHONE] = account.phone
            it[Keys.PASSWORD_HASH] = account.passwordHash
            it[Keys.HPHT] = account.hpht
            it[Keys.TRIMESTER] = account.trimester
        }
    }
}

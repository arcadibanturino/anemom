package com.kelompok.anemom.data.repository

import com.kelompok.anemom.data.local.SessionManager
import com.kelompok.anemom.data.local.UserAccount
import com.kelompok.anemom.data.remote.ApiService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.security.MessageDigest

/**
 * Repository autentikasi.
 *
 * Untuk checkpoint ini login/register berjalan secara LOKAL (DataStore) supaya alur
 * Login -> Beranda -> Logout bisa didemokan. Hanya satu akun yang disimpan per perangkat.
 *
 * TODO(backend): ganti isi login/register/logout agar memanggil [api]
 * (atau Firebase Auth), lalu simpan token sesi, bukan hash kata sandi.
 */
class AuthRepository(
    private val session: SessionManager,
    @Suppress("unused") private val api: ApiService
) {

    val onboardingDone: Flow<Boolean> = session.onboardingDone
    val isLoggedIn: Flow<Boolean> = session.isLoggedIn
    val account: Flow<UserAccount?> = session.account

    suspend fun setOnboardingDone() = session.setOnboardingDone()

    suspend fun register(account: UserAccount, password: String): Result<Unit> {
        // TODO(backend): api.register(RegisterRequest(...))
        return runCatching {
            session.saveAccount(account.copy(passwordHash = hash(password)))
            session.setLoggedIn(true)
        }
    }

    suspend fun login(email: String, password: String): Result<Unit> {
        // TODO(backend): api.login(LoginRequest(email, password))
        val saved = session.account.first()
            ?: return Result.failure(IllegalStateException("Akun belum terdaftar. Silakan daftar terlebih dahulu."))

        val valid = saved.email.equals(email.trim(), ignoreCase = true) &&
            saved.passwordHash == hash(password)
        if (!valid) {
            return Result.failure(IllegalStateException("Email atau kata sandi salah."))
        }
        session.setLoggedIn(true)
        return Result.success(Unit)
    }

    suspend fun logout() {
        // TODO(backend): hapus token sesi / panggil endpoint logout
        session.setLoggedIn(false)
    }

    private fun hash(value: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray())
            .joinToString("") { "%02x".format(it) }
}

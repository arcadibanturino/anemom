package com.kelompok.anemom.data.remote

import com.kelompok.anemom.data.local.SessionManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Menempelkan token sesi (`Authorization: Bearer ...`) ke setiap permintaan.
 * Jika server menolak token yang kita kirim (kedaluwarsa/dicabut), sesi lokal dihapus;
 * `SessionGuard` lalu mengembalikan pengguna ke halaman Masuk.
 *
 * Berjalan di thread OkHttp (bukan thread utama), jadi runBlocking di sini aman.
 */
class AuthInterceptor(private val session: SessionManager) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val token = runBlocking { session.token.first() }

        val request = chain.request().newBuilder()
            .header("Accept", "application/json")
            .apply { if (token != null) header("Authorization", "Bearer $token") }
            .build()

        val response = chain.proceed(request)

        if (token != null && response.code == 401) {
            val body = runCatching { response.peekBody(2048).string() }.getOrDefault("")
            if ("invalid_token" in body || "unauthenticated" in body) {
                runBlocking { session.clearSession() }
            }
        }
        return response
    }
}

package com.kelompok.anemom.data.remote

import com.kelompok.anemom.data.remote.dto.AuthResponse
import com.kelompok.anemom.data.remote.dto.LoginRequest
import com.kelompok.anemom.data.remote.dto.RegisterRequest
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * Kontrak endpoint backend.
 *
 * TODO(backend): endpoint di bawah masih contoh dan BELUM dipanggil oleh aplikasi.
 * Tambahkan endpoint lain (riwayat Hb, hasil scan mata, data band/PPG, dst.) di sini.
 */
interface ApiService {

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): AuthResponse

    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): AuthResponse
}

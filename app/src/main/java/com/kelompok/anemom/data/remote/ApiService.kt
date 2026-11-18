package com.kelompok.anemom.data.remote

import com.google.gson.JsonElement
import com.kelompok.anemom.data.remote.dto.AuthData
import com.kelompok.anemom.data.remote.dto.DashboardDto
import com.kelompok.anemom.data.remote.dto.FoodDto
import com.kelompok.anemom.data.remote.dto.HbCreateRequest
import com.kelompok.anemom.data.remote.dto.HbResultDto
import com.kelompok.anemom.data.remote.dto.LoginRequest
import com.kelompok.anemom.data.remote.dto.PpgRequest
import com.kelompok.anemom.data.remote.dto.RegisterRequest
import com.kelompok.anemom.data.remote.dto.ScreeningDto
import com.kelompok.anemom.data.remote.dto.TtdMonthDto
import com.kelompok.anemom.data.remote.dto.TtdSettingsDto
import com.kelompok.anemom.data.remote.dto.TtdSettingsRequest
import com.kelompok.anemom.data.remote.dto.TtdTakeDto
import com.kelompok.anemom.data.remote.dto.TtdTakeRequest
import com.kelompok.anemom.data.remote.dto.UpdateProfileRequest
import com.kelompok.anemom.data.remote.dto.UserDto
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.PartMap
import retrofit2.http.Query

/**
 * Kontrak endpoint backend (lihat docs/API.md). Base URL: https://<domain>/api/v1/
 * Semua respons dibungkus [ApiEnvelope]; panggil lewat [apiCall] supaya galat jadi [Result].
 */
interface ApiService {

    // ---- Akun ----

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): ApiEnvelope<AuthData>

    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): ApiEnvelope<AuthData>

    @POST("auth/logout")
    suspend fun logout(): ApiEnvelope<JsonElement>

    @GET("me")
    suspend fun me(): ApiEnvelope<UserDto>

    @PUT("me")
    suspend fun updateMe(@Body request: UpdateProfileRequest): ApiEnvelope<UserDto>

    // ---- Beranda ----

    @GET("dashboard")
    suspend fun dashboard(): ApiEnvelope<DashboardDto>

    // ---- Hb laboratorium ----

    @GET("hb-results")
    suspend fun hbResults(@Query("limit") limit: Int = 200): ApiEnvelope<List<HbResultDto>>

    @POST("hb-results")
    suspend fun addHb(@Body request: HbCreateRequest): ApiEnvelope<HbResultDto>

    // ---- TTD ----

    @GET("ttd")
    suspend fun ttdMonth(@Query("month") month: String): ApiEnvelope<TtdMonthDto>

    @POST("ttd/take")
    suspend fun ttdTake(@Body request: TtdTakeRequest = TtdTakeRequest()): ApiEnvelope<TtdTakeDto>

    @GET("ttd/settings")
    suspend fun ttdSettings(): ApiEnvelope<TtdSettingsDto>

    @PUT("ttd/settings")
    suspend fun updateTtdSettings(@Body request: TtdSettingsRequest): ApiEnvelope<TtdSettingsDto>

    // ---- Nutrisi ----

    /** Bahan yang direkomendasikan (is_recommended = 1), sudah berurutan dari server. Bisa kosong. */
    @GET("recommendations")
    suspend fun recommendations(): ApiEnvelope<List<FoodDto>>

    // ---- Skrining ----

    @GET("screenings")
    suspend fun screenings(
        @Query("method") method: String? = null,
        @Query("limit") limit: Int = 20
    ): ApiEnvelope<List<ScreeningDto>>

    /** Field teks (`captured_at`, `client_uuid`, `on_device_result`) lewat [fields]. */
    @Multipart
    @POST("screenings/eye")
    suspend fun createEyeScreening(
        @Part image: MultipartBody.Part,
        @PartMap fields: Map<String, @JvmSuppressWildcards RequestBody>
    ): ApiEnvelope<ScreeningDto>

    @POST("screenings/ppg")
    suspend fun createPpgScreening(@Body request: PpgRequest): ApiEnvelope<ScreeningDto>
}

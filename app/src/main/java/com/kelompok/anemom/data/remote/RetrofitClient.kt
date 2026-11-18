package com.kelompok.anemom.data.remote

import com.kelompok.anemom.BuildConfig
import com.kelompok.anemom.data.local.SessionManager
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    /** [baseUrl] harus diakhiri "/" (mis. https://anemom.sovereigns.my.id/api/v1/). */
    fun create(baseUrl: String, session: SessionManager): ApiService {
        val logging = HttpLoggingInterceptor().apply {
            // BASIC saja (metode, URL, kode status). Level BODY akan mencetak kata sandi
            // dan data kesehatan ke Logcat.
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BASIC
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
            redactHeader("Authorization")
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(session))
            .addInterceptor(logging)
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)   // unggah foto + inferensi di server bisa lebih lama
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}

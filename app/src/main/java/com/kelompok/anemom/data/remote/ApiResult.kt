package com.kelompok.anemom.data.remote

import com.google.gson.Gson
import com.google.gson.JsonElement
import kotlinx.coroutines.CancellationException
import retrofit2.HttpException
import java.io.IOException

const val NETWORK_ERROR_MESSAGE = "Tidak dapat terhubung ke server. Periksa koneksi internet Bunda."

/** Pembungkus respons sukses backend: {"data": ..., "meta": {...}}. */
data class ApiEnvelope<T>(
    val data: T? = null,
    val meta: JsonElement? = null
)

/**
 * Galat dari backend ({"error": {"code", "message", "details"}}) atau galat jaringan.
 * [code] mis. `invalid_credentials`, `email_taken`, `validation_failed`, `network_error`.
 */
class ApiException(
    val code: String,
    message: String,
    val details: Map<String, String> = emptyMap(),
    val httpStatus: Int = 0
) : Exception(message)

private data class ErrorEnvelope(val error: ErrorBody? = null)

private data class ErrorBody(
    val code: String? = null,
    val message: String? = null,
    val details: JsonElement? = null
)

private val gson = Gson()

/**
 * Menjalankan satu panggilan API dan mengubah hasilnya menjadi [Result].
 * Tidak pernah melempar galat jaringan/HTTP; pesan galat sudah berbahasa Indonesia
 * dan siap ditampilkan ke pengguna.
 */
suspend fun <T> apiCall(block: suspend () -> ApiEnvelope<T>): Result<T> = try {
    val data = block().data ?: throw ApiException("empty_response", "Respons server kosong.")
    Result.success(data)
} catch (e: CancellationException) {
    throw e
} catch (e: ApiException) {
    Result.failure(e)
} catch (e: HttpException) {
    Result.failure(e.toApiException())
} catch (e: IOException) {
    Result.failure(ApiException("network_error", NETWORK_ERROR_MESSAGE))
} catch (e: Exception) {
    Result.failure(ApiException("unknown", "Terjadi kesalahan saat membaca respons server."))
}

private fun HttpException.toApiException(): ApiException {
    val raw = runCatching { response()?.errorBody()?.string() }.getOrNull()
    val body = runCatching { gson.fromJson(raw, ErrorEnvelope::class.java) }.getOrNull()?.error

    val details: Map<String, String> = body?.details
        ?.takeIf { it.isJsonObject }
        ?.asJsonObject
        ?.entrySet()
        ?.associate { (key, value) -> key to (if (value.isJsonPrimitive) value.asString else value.toString()) }
        .orEmpty()

    val message = when {
        body == null && code() >= 500 -> "Server sedang bermasalah (kode ${code()}). Coba lagi nanti."
        body == null -> "Permintaan ditolak server (kode ${code()})."
        body.code == "validation_failed" && details.isNotEmpty() -> details.values.joinToString(" ")
        else -> body.message ?: "Terjadi kesalahan (kode ${code()})."
    }
    return ApiException(body?.code ?: "http_${code()}", message, details, code())
}

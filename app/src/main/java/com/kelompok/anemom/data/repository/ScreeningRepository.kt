package com.kelompok.anemom.data.repository

import com.google.gson.Gson
import com.kelompok.anemom.data.AppEvents
import com.kelompok.anemom.data.remote.ApiService
import com.kelompok.anemom.data.remote.apiCall
import com.kelompok.anemom.data.remote.dto.EyeOnDeviceResultDto
import com.kelompok.anemom.data.remote.dto.PpgRequest
import com.kelompok.anemom.data.remote.dto.ScreeningDto
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.time.Instant
import java.util.UUID

/** Satu hasil skrining (AI Scan Mata atau PPG band) dari server. */
data class Screening(
    val id: Long,
    /** `ai_eye` | `ppg_band` */
    val method: String,
    /** `done` | `queued` | `processing` | `failed` */
    val status: String,
    val measuredAt: String?,
    /** `anemia` | `non_anemia` */
    val label: String?,
    val estHb: Float?,
    val confidence: Float?,
    /** Peluang anemia menurut model (0..1); lebih bermakna untuk ditampilkan daripada [confidence]. */
    val score: Float?,
    /** Batas skrining yang dipakai model; null untuk hasil lama. */
    val scoreThreshold: Float?,
    val inferenceSource: String?,
    val pregnancyWeek: Int? = null
)

/**
 * Skrining ke server (POST /screenings/eye, /screenings/ppg, GET /screenings).
 *
 * [submitEye] dipakai layar AI Scan Mata (foto sudah dipotong sesuai kotak panduan dan lolos
 * pemeriksaan U-Net di HP). [submitPpg] belum dipakai: Smartband masih simulasi, jadi jangan
 * mengirim data simulasi ke server; panggil saat band asli tersambung.
 */
class ScreeningRepository(
    private val api: ApiService,
    private val events: AppEvents
) {
    private val gson = Gson()

    /** Riwayat skrining untuk tab Riwayat; ikut dimuat ulang setelah ada skrining baru. */
    private val historySource = RemoteSource(events) { recent(limit = 50) }
    val history: kotlinx.coroutines.flow.Flow<List<Screening>> = historySource.flow
    val historyError: kotlinx.coroutines.flow.StateFlow<String?> = historySource.error

    suspend fun recent(method: String? = null, limit: Int = 20): Result<List<Screening>> =
        apiCall { api.screenings(method, limit) }.map { list -> list.map { it.toModel() } }

    /**
     * Kirim foto mata.
     * [onDevice] diisi jika model di HP sudah menghitung (server hanya menyimpan).
     * Jika null, server menjalankan model segmentasi + klasifikasi di VPS; hasilnya bisa berstatus
     * `queued`/`processing` (HTTP 202) dan perlu dipoll lewat [recent].
     * [segmenter] (`unet` / `yolo`, dari SegmenterInfo.segmenter) memberi tahu server model segmentasi
     * mana yang dipakai, supaya hasil flavor U-Net dan YOLO tercatat terpisah di database yang sama.
     */
    suspend fun submitEye(
        image: File,
        onDevice: EyeOnDeviceResultDto? = null,
        segmenter: String? = null
    ): Result<Screening> {
        val text = "text/plain".toMediaType()
        val fields = mutableMapOf<String, RequestBody>(
            "captured_at" to Instant.now().toString().toRequestBody(text),
            "client_uuid" to UUID.randomUUID().toString().toRequestBody(text)
        )
        if (segmenter != null) {
            fields["segmenter"] = segmenter.toRequestBody(text)
        }
        if (onDevice != null) {
            fields["on_device_result"] = gson.toJson(onDevice).toRequestBody("application/json".toMediaType())
        }
        val part = MultipartBody.Part.createFormData(
            "image",
            image.name,
            image.asRequestBody("image/jpeg".toMediaType())
        )
        return apiCall { api.createEyeScreening(part, fields) }
            .map { events.invalidateData(); it.toModel() }
    }

    /** Kirim hasil rekaman PPG dari band asli. */
    suspend fun submitPpg(request: PpgRequest): Result<Screening> =
        apiCall { api.createPpgScreening(request) }
            .map { events.invalidateData(); it.toModel() }
}

private fun ScreeningDto.toModel() = Screening(
    id = id,
    method = method,
    status = status,
    measuredAt = measuredAt,
    label = label,
    estHb = estHb?.toFloat(),
    confidence = confidence?.toFloat(),
    score = score?.toFloat(),
    scoreThreshold = scoreThreshold?.toFloat(),
    inferenceSource = inferenceSource,
    pregnancyWeek = pregnancyWeek
)

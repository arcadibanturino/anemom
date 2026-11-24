package com.kelompok.anemom.data.remote.dto

import com.google.gson.annotations.SerializedName

data class ScreeningDto(
    val id: Long = 0,
    /** `ai_eye` | `ppg_band` */
    val method: String = "",
    /** `done` | `queued` | `processing` | `failed` */
    val status: String = "done",
    @SerializedName("measured_at") val measuredAt: String? = null,
    @SerializedName("pregnancy_week") val pregnancyWeek: Int? = null,
    /** `anemia` | `non_anemia` */
    val label: String? = null,
    @SerializedName("est_hb") val estHb: Double? = null,
    val risk: String? = null,
    val confidence: Double? = null,
    /** Peluang anemia menurut model (0..1). */
    val score: Double? = null,
    /** Batas skrining: score >= batas -> label `anemia`. */
    @SerializedName("score_threshold") val scoreThreshold: Double? = null,
    @SerializedName("inference_source") val inferenceSource: String? = null
)

/**
 * Hasil model yang dihitung di HP. Dikirim sebagai JSON di field `on_device_result`.
 * [modelVersion] = versi model KLASIFIKASI; [segModelVersion] = versi model SEGMENTASI (U-Net/YOLO).
 */
data class EyeOnDeviceResultDto(
    val label: String,
    val confidence: Float,
    @SerializedName("est_hb") val estHb: Float? = null,
    @SerializedName("model_version") val modelVersion: String? = null,
    /** `unet` | `yolo` */
    val segmenter: String? = null,
    @SerializedName("seg_model_version") val segModelVersion: String? = null
)

data class PpgResultDto(
    val label: String? = null,
    @SerializedName("est_hb") val estHb: Float? = null,
    val confidence: Float? = null,
    @SerializedName("model_version") val modelVersion: String? = null,
    @SerializedName("inference_source") val inferenceSource: String? = null
)

data class PpgRequest(
    @SerializedName("client_uuid") val clientUuid: String,
    @SerializedName("device_uid") val deviceUid: String? = null,
    @SerializedName("started_at") val startedAt: String,
    @SerializedName("duration_s") val durationSeconds: Int,
    @SerializedName("sample_rate_hz") val sampleRateHz: Int,
    @SerializedName("sample_count") val sampleCount: Int? = null,
    @SerializedName("hr_bpm") val heartRate: Int? = null,
    @SerializedName("spo2_pct") val spo2: Float? = null,
    /** `buruk` | `cukup` | `baik` */
    @SerializedName("signal_quality") val signalQuality: String? = null,
    @SerializedName("quality_score") val qualityScore: Float? = null,
    val features: Map<String, Float>? = null,
    val result: PpgResultDto? = null
)

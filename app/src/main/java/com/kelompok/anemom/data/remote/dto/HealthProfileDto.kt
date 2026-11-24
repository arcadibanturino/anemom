package com.kelompok.anemom.data.remote.dto

import com.google.gson.annotations.SerializedName

// Kontrak: GET/PUT /me/health-profile (docs/API.md).

/** Isi `data` dari GET /me/health-profile: isi form + bahan alergi yang sudah dicentang. */
data class HealthProfileDto(
    @SerializedName("height_cm") val heightCm: Double? = null,
    @SerializedName("pre_pregnancy_weight_kg") val preWeightKg: Double? = null,
    /** `ya` | `tidak` | `tidak_tahu` | null (belum diisi) */
    @SerializedName("anemia_history") val anemiaHistory: String? = null,
    /** `ada` | `tidak` | `tidak_tahu` | null (belum diisi) */
    @SerializedName("allergy_status") val allergyStatus: String? = null,
    @SerializedName("lila_cm") val lilaCm: Double? = null,
    @SerializedName("allergen_ids") val allergenIds: List<Long> = emptyList(),
    @SerializedName("profile_completed") val profileCompleted: Boolean = false
)

/**
 * Isi PUT /me/health-profile. Disimpan lengkap, bukan sebagian.
 * [lilaCm] null = tidak dikirim (LiLA opsional; server menghapus LiLA lama).
 * [allergenIds] hanya dipakai server bila [allergyStatus] = `ada`.
 */
data class HealthProfileRequest(
    @SerializedName("height_cm") val heightCm: Double,
    @SerializedName("pre_pregnancy_weight_kg") val preWeightKg: Double,
    @SerializedName("anemia_history") val anemiaHistory: String,
    @SerializedName("allergy_status") val allergyStatus: String,
    @SerializedName("lila_cm") val lilaCm: Double? = null,
    @SerializedName("allergen_ids") val allergenIds: List<Long> = emptyList()
)

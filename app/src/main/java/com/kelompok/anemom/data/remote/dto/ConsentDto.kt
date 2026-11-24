package com.kelompok.anemom.data.remote.dto

import com.google.gson.annotations.SerializedName

/** GET/POST /consents. `type` = privacy_policy | health_data | image_upload | research_use. */
data class ConsentDto(
    val type: String = "",
    val version: String = "",
    @SerializedName("accepted_at") val acceptedAt: String? = null,
    @SerializedName("revoked_at") val revokedAt: String? = null
)

data class ConsentRequest(
    val type: String,
    val version: String,
    val accepted: Boolean = true
)

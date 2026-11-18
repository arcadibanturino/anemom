package com.kelompok.anemom.data.remote.dto

import com.google.gson.annotations.SerializedName

data class FoodDto(
    val id: Long = 0,
    val name: String = "",
    val category: String? = null,
    val description: String? = null,
    /** `heme` | `non_heme` | null */
    @SerializedName("iron_type") val ironType: String? = null,
    /** Path relatif di server (mis. /api/v1/ingredients/3/image); null = belum ada foto. */
    @SerializedName("image_url") val imageUrl: String? = null,
    @SerializedName("serving_desc") val servingDesc: String? = null,
    @SerializedName("iron_mg") val ironMg: Double? = null,
    @SerializedName("is_iron_booster") val isIronBooster: Boolean = false,
    @SerializedName("is_iron_inhibitor") val isIronInhibitor: Boolean = false,
    /** Peringatan khusus ibu hamil (mis. hati ayam tinggi vitamin A); null = tidak ada. */
    @SerializedName("pregnancy_warning") val pregnancyWarning: String? = null
)

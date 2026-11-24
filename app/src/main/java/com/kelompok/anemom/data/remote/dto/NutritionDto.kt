package com.kelompok.anemom.data.remote.dto

import com.google.gson.annotations.SerializedName

data class FoodDto(
    val id: Long = 0,
    val name: String = "",
    val category: String? = null,
    /** `sayur` | `buah` | `protein` | `lainnya` (dihitung server dari category). Server lama: null. */
    val group: String? = null,
    /** Label kecil di detail, mis. "Sayuran hijau", "Protein nabati". */
    @SerializedName("subcategory_label") val subcategoryLabel: String? = null,
    /** Teks singkat kartu di daftar. */
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
    @SerializedName("pregnancy_warning") val pregnancyWarning: String? = null,
    // ---- Detail bahan (null = belum diisi) ----
    /** Judul sorotan, mis. "Folat & zat besi non-heme". */
    @SerializedName("key_nutrients") val keyNutrients: String? = null,
    /** Manfaat Nutrisi. */
    val benefit: String? = null,
    /** Cara memilih & menyiapkan. */
    val preparation: String? = null,
    /** Bagian alergi di "Sesuai risiko & alergi". */
    @SerializedName("allergy_note") val allergyNote: String? = null,
    /** true = ada halaman detail ("Lihat Detail"). */
    @SerializedName("has_detail") val hasDetail: Boolean = false
)

/**
 * `meta` dari GET /recommendations dan GET /ingredients/{id}: konteks ibu untuk menyesuaikan saran.
 * Kunci lain di meta (mis. `count`) diabaikan.
 */
data class FoodContextDto(
    /** Kode hb_risk_levels dari Hb lab terakhir (`normal` | `moderate` | `high`); null = belum ada hasil. */
    val risk: String? = null,
    @SerializedName("risk_label") val riskLabel: String? = null,
    /** `ada` | `tidak` | `tidak_tahu` | null */
    @SerializedName("allergy_status") val allergyStatus: String? = null,
    /** Nama bahan yang dicentang sebagai alergi (sudah dikecualikan server). */
    val allergens: List<String>? = null
)

package com.kelompok.anemom.data.model

/** Satu bahan makanan dari katalog server (GET /recommendations, GET /ingredients/{id}). */
data class FoodItem(
    val name: String,
    val category: String,
    val description: String?,
    /** `heme` | `non_heme` | null */
    val ironType: String?,
    /** URL gambar lengkap; null = belum ada foto. */
    val imageUrl: String?,
    val servingDesc: String,
    /** Zat besi (mg) per [servingDesc]; null jika belum ada datanya. */
    val ironMg: Double?,
    /** Pendukung serapan zat besi (mis. vitamin C). */
    val isIronBooster: Boolean,
    /** Penghambat serapan zat besi (mis. teh, kopi, susu). */
    val isIronInhibitor: Boolean,
    /** Peringatan untuk ibu hamil dari server; null = tidak ada. */
    val pregnancyWarning: String?,
    val id: Long = 0,
    /** Tab di layar Rekomendasi Makanan. */
    val group: FoodGroup = FoodGroup.LAINNYA,
    /** Label kecil di detail, mis. "Sayuran hijau". */
    val subcategoryLabel: String? = null,
    // ---- Detail bahan ----
    val keyNutrients: String? = null,
    val benefit: String? = null,
    val preparation: String? = null,
    val allergyNote: String? = null,
    /** true = tampil "Lihat Detail" dan halaman detail bisa dibuka. */
    val hasDetail: Boolean = false
)

/** Konteks ibu dari server untuk menyesuaikan saran makanan (kartu "Sesuai kebutuhan Bunda"). */
data class FoodContext(
    /** Kategori Hb lab terakhir; null = belum ada hasil (atau kode yang belum dikenal aplikasi). */
    val risk: HbRisk?,
    /** Label dari server (hb_risk_levels.label); dipakai apa adanya untuk tampilan. */
    val riskLabel: String?,
    val allergyStatus: AllergyStatus?,
    /** Nama bahan alergi; bahan ini sudah tidak ada di daftar rekomendasi. */
    val allergens: List<String>
) {
    companion object {
        val EMPTY = FoodContext(risk = null, riskLabel = null, allergyStatus = null, allergens = emptyList())
    }
}

/** Isi layar Rekomendasi Makanan. */
data class FoodRecommendations(
    val items: List<FoodItem>,
    val context: FoodContext
)

/** Isi layar Detail Bahan Makanan. */
data class FoodDetail(
    val item: FoodItem,
    val context: FoodContext
)

package com.kelompok.anemom.data.model

/** Satu bahan makanan dari katalog server (GET /recommendations). Nilai gizi per [servingDesc]. */
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
    val pregnancyWarning: String?
)

package com.kelompok.anemom.ui.nutrisi

import com.kelompok.anemom.data.model.FoodItem

/** Satu kartu rekomendasi bahan makanan di Beranda dan di daftar "Lihat Semua". Semua isinya dari server. */
data class MenuRecommendation(
    val title: String,
    /** Label singkat: jenis zat besi atau peran bahan (mis. "Sumber zat besi hewani"). */
    val tag: String,
    val description: String?,
    /** URL gambar lengkap dari server; null = belum ada foto, kartu menampilkan ikon pengganti. */
    val imageUrl: String?,
    /** Peringatan untuk ibu hamil dari server; null = tidak ada. */
    val warning: String? = null
)

/**
 * Rekomendasi dari server (GET /recommendations) -> kartu.
 * Urutan dari server dipertahankan; bahan ber-peringatan ditaruh paling akhir.
 * Tidak ada takaran/porsi: jumlah konsumsi ditentukan tenaga kesehatan, bukan aplikasi.
 */
fun List<FoodItem>.toMenuRecommendations(): List<MenuRecommendation> =
    map { it.toMenuRecommendation() }
        .sortedBy { it.warning != null }

private fun FoodItem.toMenuRecommendation() = MenuRecommendation(
    title = name,
    tag = tagText(),
    description = description,
    imageUrl = imageUrl,
    warning = pregnancyWarning
)

private fun FoodItem.tagText(): String = when {
    isIronBooster -> "Kaya vitamin C, bantu serapan zat besi"
    ironType == "heme" -> "Sumber zat besi hewani"
    ironType == "non_heme" -> "Sumber zat besi nabati"
    else -> category.replaceFirstChar { it.uppercase() }
}

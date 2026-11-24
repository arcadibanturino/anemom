package com.kelompok.anemom.data.model

/** Riwayat anemia pada layar Profil Kesehatan Awal. [code] = nilai di server. */
enum class AnemiaHistory(val code: String, val label: String) {
    YA("ya", "Ya"),
    TIDAK("tidak", "Tidak"),
    TIDAK_TAHU("tidak_tahu", "Tidak Tahu");

    companion object {
        fun fromCode(code: String?): AnemiaHistory? = entries.firstOrNull { it.code == code }
    }
}

/** Status alergi makanan. [ADA] membuka daftar bahan yang bisa dicentang. */
enum class AllergyStatus(val code: String, val label: String) {
    ADA("ada", "Ada"),
    TIDAK("tidak", "Tidak"),
    TIDAK_TAHU("tidak_tahu", "Tidak Tahu");

    companion object {
        fun fromCode(code: String?): AllergyStatus? = entries.firstOrNull { it.code == code }
    }
}

/**
 * Kelompok bahan makanan di aplikasi (tab Sayur / Buah / Protein). Pemetaan dari kategori database
 * dilakukan server (`group`); [fromServer] hanya cadangan untuk server yang belum mengirim `group`.
 */
enum class FoodGroup(val label: String) {
    SAYUR("Sayur"),
    BUAH("Buah"),
    PROTEIN("Protein"),
    LAINNYA("Lainnya");

    companion object {
        fun fromServer(group: String?, category: String?): FoodGroup {
            val key = group ?: when (category) {
                "hewani", "nabati" -> "protein"
                else -> category
            }
            return when (key) {
                "sayur" -> SAYUR
                "buah" -> BUAH
                "protein" -> PROTEIN
                else -> LAINNYA
            }
        }
    }
}

/** Satu bahan pada checklist alergi. */
data class AllergenOption(
    val id: Long,
    val name: String,
    val group: FoodGroup
)

/** Isi form Profil Kesehatan Awal yang sudah tersimpan di server (untuk melanjutkan pengisian). */
data class HealthProfile(
    val heightCm: Double?,
    val preWeightKg: Double?,
    val anemiaHistory: AnemiaHistory?,
    val allergyStatus: AllergyStatus?,
    val lilaCm: Double?,
    val allergenIds: Set<Long>
)

/** Data yang dikirim saat menyimpan; semua wajib kecuali [lilaCm]. */
data class HealthProfileInput(
    val heightCm: Double,
    val preWeightKg: Double,
    val anemiaHistory: AnemiaHistory,
    val allergyStatus: AllergyStatus,
    val lilaCm: Double?,
    /** Hanya dikirim bila [allergyStatus] = ADA. */
    val allergenIds: Set<Long>
)

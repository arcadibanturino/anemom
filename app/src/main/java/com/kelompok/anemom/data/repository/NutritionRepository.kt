package com.kelompok.anemom.data.repository

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.kelompok.anemom.BuildConfig
import com.kelompok.anemom.data.AppEvents
import com.kelompok.anemom.data.model.AllergenOption
import com.kelompok.anemom.data.model.AllergyStatus
import com.kelompok.anemom.data.model.FoodContext
import com.kelompok.anemom.data.model.FoodDetail
import com.kelompok.anemom.data.model.FoodGroup
import com.kelompok.anemom.data.model.FoodItem
import com.kelompok.anemom.data.model.FoodRecommendations
import com.kelompok.anemom.data.model.HbRisk
import com.kelompok.anemom.data.remote.ApiEnvelope
import com.kelompok.anemom.data.remote.ApiService
import com.kelompok.anemom.data.remote.apiCall
import com.kelompok.anemom.data.remote.dto.FoodContextDto
import com.kelompok.anemom.data.remote.dto.FoodDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import java.net.URI

/**
 * Rekomendasi bahan makanan dari server (GET /recommendations), sudah diurutkan server,
 * beserta konteks ibu (risiko Hb terakhir dan alergi) dari `meta`.
 */
class NutritionRepository(
    private val api: ApiService,
    private val events: AppEvents
) {
    private val remote = RemoteSource(events) {
        apiCall {
            val envelope = api.recommendations()
            ApiEnvelope(
                FoodRecommendations(
                    items = envelope.data.orEmpty().map { it.toModel() },
                    context = envelope.meta.toFoodContext()
                )
            )
        }
    }

    /** Layar Rekomendasi Makanan: bahan + konteks ibu. */
    val recommendations: Flow<FoodRecommendations> = remote.flow

    /** Pesan galat terakhir saat memuat rekomendasi; null = berhasil / belum dimuat. */
    val recommendationsError: StateFlow<String?> = remote.error

    /** Beranda: hanya daftar bahannya. */
    val foods: Flow<List<FoodItem>> = recommendations.map { it.items }

    /** Muat ulang data dari server (dipakai tombol "Coba lagi"). */
    fun refresh() = events.invalidateData()

    /** Satu bahan untuk layar Detail Bahan Makanan (GET /ingredients/{id}). */
    suspend fun ingredientDetail(id: Long): Result<FoodDetail> =
        apiCall {
            val envelope = api.ingredient(id)
            ApiEnvelope(envelope.data?.let { FoodDetail(it.toModel(), envelope.meta.toFoodContext()) })
        }

    /** Seluruh bahan di katalog server untuk checklist alergi (GET /ingredients), tanpa filter rekomendasi. */
    suspend fun allergenOptions(): Result<List<AllergenOption>> =
        apiCall { api.ingredients() }.map { list ->
            list.map { AllergenOption(id = it.id, name = it.name, group = FoodGroup.fromServer(it.group, it.category)) }
        }
}

private val gson = Gson()

private fun FoodDto.toModel() = FoodItem(
    name = name,
    category = category.orEmpty(),
    description = description.clean(),
    ironType = ironType,
    imageUrl = absoluteUrl(imageUrl),
    servingDesc = servingDesc.orEmpty().ifBlank { "porsi" },
    ironMg = ironMg,
    isIronBooster = isIronBooster,
    isIronInhibitor = isIronInhibitor,
    pregnancyWarning = pregnancyWarning.clean(),
    id = id,
    group = FoodGroup.fromServer(group, category),
    subcategoryLabel = subcategoryLabel.clean(),
    keyNutrients = keyNutrients.clean(),
    benefit = benefit.clean(),
    preparation = preparation.clean(),
    allergyNote = allergyNote.clean(),
    // Server lama belum mengirim has_detail: anggap ada detail bila "Manfaat Nutrisi" terisi.
    hasDetail = hasDetail || !benefit.isNullOrBlank()
)

private fun String?.clean(): String? = this?.trim()?.ifBlank { null }

private fun JsonElement?.toFoodContext(): FoodContext {
    if (this == null || !isJsonObject) return FoodContext.EMPTY
    val dto = runCatching { gson.fromJson(this, FoodContextDto::class.java) }.getOrNull()
        ?: return FoodContext.EMPTY
    return FoodContext(
        risk = when (dto.risk) {
            "normal" -> HbRisk.NORMAL
            "moderate" -> HbRisk.MODERATE
            "high" -> HbRisk.HIGH
            else -> null
        },
        riskLabel = dto.riskLabel.clean(),
        allergyStatus = AllergyStatus.fromCode(dto.allergyStatus),
        allergens = dto.allergens.orEmpty().mapNotNull { it.clean() }
    )
}

/** image_url dari server berbentuk path (/api/v1/...); dilengkapi dengan alamat server dari BASE_URL. */
private fun absoluteUrl(path: String?): String? {
    if (path.isNullOrBlank()) return null
    if (path.startsWith("http://") || path.startsWith("https://")) return path
    val base = runCatching { URI(BuildConfig.BASE_URL) }.getOrNull() ?: return null
    val scheme = base.scheme ?: return null
    val authority = base.authority ?: return null
    return "$scheme://$authority" + if (path.startsWith("/")) path else "/$path"
}

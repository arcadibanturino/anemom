package com.kelompok.anemom.data.repository

import com.kelompok.anemom.BuildConfig
import com.kelompok.anemom.data.AppEvents
import com.kelompok.anemom.data.model.FoodItem
import com.kelompok.anemom.data.remote.ApiService
import com.kelompok.anemom.data.remote.apiCall
import com.kelompok.anemom.data.remote.dto.FoodDto
import kotlinx.coroutines.flow.Flow
import java.net.URI

/**
 * Rekomendasi bahan makanan dari server (GET /recommendations), sudah diurutkan server.
 * Daftar kosong sampai data diisi di database; layar memakai isi bawaan saat itu.
 */
class NutritionRepository(
    api: ApiService,
    events: AppEvents
) {
    private val remote = RemoteSource(events) {
        apiCall { api.recommendations() }.map { list -> list.map { it.toModel() } }
    }

    val foods: Flow<List<FoodItem>> = remote.flow
}

private fun FoodDto.toModel() = FoodItem(
    name = name,
    category = category.orEmpty(),
    description = description?.trim()?.ifBlank { null },
    ironType = ironType,
    imageUrl = absoluteUrl(imageUrl),
    servingDesc = servingDesc.orEmpty().ifBlank { "porsi" },
    ironMg = ironMg,
    isIronBooster = isIronBooster,
    isIronInhibitor = isIronInhibitor,
    pregnancyWarning = pregnancyWarning?.trim()?.ifBlank { null }
)

/** image_url dari server berbentuk path (/api/v1/...); dilengkapi dengan alamat server dari BASE_URL. */
private fun absoluteUrl(path: String?): String? {
    if (path.isNullOrBlank()) return null
    if (path.startsWith("http://") || path.startsWith("https://")) return path
    val base = runCatching { URI(BuildConfig.BASE_URL) }.getOrNull() ?: return null
    val scheme = base.scheme ?: return null
    val authority = base.authority ?: return null
    return "$scheme://$authority" + if (path.startsWith("/")) path else "/$path"
}

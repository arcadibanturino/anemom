package com.kelompok.anemom.data.repository

import com.kelompok.anemom.data.AppEvents
import com.kelompok.anemom.data.model.HbResult
import com.kelompok.anemom.data.remote.ApiService
import com.kelompok.anemom.data.remote.apiCall
import com.kelompok.anemom.data.remote.dto.HbCreateRequest
import com.kelompok.anemom.data.remote.dto.HbResultDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.util.UUID

/**
 * Hasil Hb darah dari server (GET/POST /hb-results), urut dari tanggal terlama ke terbaru.
 * Kategori risiko dihitung server; aplikasi hanya menampilkannya.
 *
 * TODO(fitur): estimasi AI (skor mata / PPG) disimpan terpisah dari hasil lab (lihat [ScreeningRepository]).
 */
class HbRepository(
    private val api: ApiService,
    private val events: AppEvents
) {
    private val remote = RemoteSource(events) {
        apiCall { api.hbResults() }.map { list -> list.mapNotNull { it.toModel() } }
    }

    val results: Flow<List<HbResult>> = remote.flow.map { list -> list.sortedBy { it.date } }

    /** Pesan galat terakhir saat memuat (null jika berhasil). */
    val loadError: StateFlow<String?> = remote.error

    /** Simpan hasil Hb baru ke server. Berhasil = semua layar yang menampilkan Hb ikut diperbarui. */
    suspend fun add(value: Float, date: LocalDate, facility: String, source: String): Result<Unit> =
        apiCall {
            api.addHb(
                HbCreateRequest(
                    hbValue = value,
                    testDate = date.toString(),
                    facility = facility.trim().ifBlank { null },
                    source = source,
                    clientUuid = UUID.randomUUID().toString()
                )
            )
        }.map { events.invalidateData() }
}

private fun HbResultDto.toModel(): HbResult? {
    val date = runCatching { LocalDate.parse(testDate) }.getOrNull() ?: return null
    return HbResult(
        date = date,
        value = hbValue.toFloat(),
        facility = facility.orEmpty(),
        source = source.orEmpty()
    )
}

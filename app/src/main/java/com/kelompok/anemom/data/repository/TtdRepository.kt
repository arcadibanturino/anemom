package com.kelompok.anemom.data.repository

import com.kelompok.anemom.data.AppEvents
import com.kelompok.anemom.data.model.TtdEntry
import com.kelompok.anemom.data.remote.ApiService
import com.kelompok.anemom.data.remote.apiCall
import com.kelompok.anemom.data.remote.dto.TtdDayDto
import com.kelompok.anemom.data.remote.dto.TtdSettingsRequest
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth

/** Data TTD untuk satu bulan yang sedang ditampilkan di kalender. */
data class TtdMonthData(
    val month: YearMonth,
    /** Hari minum pada [month] (untuk kalender). */
    val entries: List<TtdEntry>,
    /** Riwayat terbaru: untuk bulan ini sudah digabung dengan bulan sebelumnya. */
    val history: List<TtdEntry>,
    val takenToday: Boolean,
    val takenLast7Days: Int,
    val percentLast7Days: Int
)

/**
 * Catatan minum Tablet Tambah Darah (TTD) dari server (GET /ttd, POST /ttd/take, /ttd/settings).
 * Kepatuhan 7 hari dihitung server.
 *
 * TODO(fitur): pengingat sungguhan (notifikasi terjadwal / FCM); saat ini hanya status aktif/tidaknya
 * dan jamnya yang disimpan di server.
 */
class TtdRepository(
    private val api: ApiService,
    private val events: AppEvents
) {
    private val settings = RemoteSource(events) { apiCall { api.ttdSettings() } }

    val reminderOn: Flow<Boolean> = settings.flow.map { it.reminderOn }

    /**
     * Data satu bulan. Mengikuti [AppEvents.dataVersion]: setelah menandai TTD, data diambil ulang.
     * Gunakan di ViewModel dengan flatMapLatest pada bulan yang dipilih.
     */
    fun month(month: YearMonth): Flow<TtdMonthData> =
        RemoteSource(events) { loadMonth(month) }.flow

    private suspend fun loadMonth(month: YearMonth): Result<TtdMonthData> = coroutineScope {
        val current = async { apiCall { api.ttdMonth(month.toString()) } }
        // Untuk bulan berjalan, ambil juga bulan lalu supaya riwayat tidak kosong di tanggal 1.
        val previous = if (month == YearMonth.now()) {
            async { apiCall { api.ttdMonth(month.minusMonths(1).toString()) } }
        } else {
            null
        }
        val currentResult = current.await()
        val previousDays = previous?.await()?.getOrNull()?.days.orEmpty()

        currentResult.map { dto ->
            val entries = dto.days.mapNotNull { it.toEntry() }
            TtdMonthData(
                month = month,
                entries = entries,
                history = (entries + previousDays.mapNotNull { it.toEntry() })
                    .sortedWith(compareByDescending<TtdEntry> { it.date }.thenByDescending { it.time }),
                takenToday = dto.takenToday,
                takenLast7Days = dto.takenLast7Days,
                percentLast7Days = dto.percentLast7Days
            )
        }
    }

    /** Tandai sudah minum hari ini (server memakai tanggal/jam saat ini). Gagal -> Toast dari [AppEvents]. */
    suspend fun markTakenToday(): Result<Unit> =
        apiCall { api.ttdTake() }
            .map { events.invalidateData() }
            .onFailure { events.showMessage(it.message ?: "Gagal menyimpan catatan TTD.") }

    suspend fun setReminder(on: Boolean): Result<Unit> =
        apiCall { api.updateTtdSettings(TtdSettingsRequest(reminderOn = on)) }
            .map { events.invalidateData() }
            .onFailure { events.showMessage(it.message ?: "Gagal mengubah pengingat.") }
}

private fun TtdDayDto.toEntry(): TtdEntry? {
    val d = runCatching { LocalDate.parse(date) }.getOrNull() ?: return null
    val t = runCatching { LocalTime.parse(time) }.getOrDefault(LocalTime.of(20, 0))
    return TtdEntry(date = d, time = t)
}

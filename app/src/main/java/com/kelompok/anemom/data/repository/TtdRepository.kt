package com.kelompok.anemom.data.repository

import com.kelompok.anemom.alarm.TtdAlarmScheduler
import com.kelompok.anemom.data.AppEvents
import com.kelompok.anemom.data.remote.ApiService
import com.kelompok.anemom.data.remote.apiCall
import com.kelompok.anemom.data.remote.dto.TtdDayDto
import com.kelompok.anemom.data.remote.dto.TtdMonthDto
import com.kelompok.anemom.data.remote.dto.TtdSettingsRequest
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth

/** Status satu hari jadwal TTD. */
enum class TtdStatus(val label: String) {
    /** Dikonfirmasi sudah diminum. */
    SUDAH("Sudah"),
    /** Dilaporkan belum diminum (dari alarm). */
    BELUM("Belum"),
    /** Belum ada jawaban. */
    BELUM_DIKONFIRMASI("Belum Dikonfirmasi")
}

data class TtdDay(
    val date: LocalDate,
    val status: TtdStatus,
    /** Jam catatan; untuk "Belum Dikonfirmasi" = jam alarm. */
    val time: LocalTime
)

/** Kepatuhan satu jendela 7 hari (grafik "Kepatuhan mingguan"). */
data class TtdWeek(
    val start: LocalDate,
    val end: LocalDate,
    val taken: Int,
    val percent: Int
)

/** Ringkasan 7 hari terakhir (hari ini + 6 hari sebelumnya), dihitung server. */
data class TtdSummary(
    val takenLast7Days: Int,
    val percentLast7Days: Int,
    val takenToday: Boolean
)

/** Pengaturan pengingat dari server. */
data class TtdReminderSettings(
    val time: LocalTime,
    val on: Boolean,
    /** false = Hb lab terakhir di bawah normal: pengingat wajib aktif. */
    val canDisable: Boolean
)

/** Isi layar Pelacak TTD. */
data class TtdTrackerData(
    /** Bulan kalender yang ditampilkan. */
    val month: YearMonth,
    val takenDates: Set<LocalDate>,
    val skippedDates: Set<LocalDate>,
    val today: TtdDay,
    /** Hari ini dulu, 7 hari. */
    val last7Days: List<TtdDay>,
    /** 4 minggu, terlama dulu; terakhir = 7 hari terakhir. */
    val weeks: List<TtdWeek>,
    val summary: TtdSummary,
    val reminderTime: LocalTime,
    val reminderOn: Boolean
)

/**
 * Catatan minum Tablet Tambah Darah (TTD) dari server: GET /ttd, POST /ttd/take, POST /ttd/skip,
 * GET/PUT /ttd/settings. Kepatuhan 7 hari dihitung server ("Sudah" ÷ 7 jadwal × 100).
 * Jam alarm di HP ([TtdAlarmScheduler]) ikut diperbarui dari data server.
 */
class TtdRepository(
    private val api: ApiService,
    private val events: AppEvents,
    private val alarm: TtdAlarmScheduler
) {
    /**
     * Data Pelacak TTD dengan kalender bulan [month]. Mengikuti [AppEvents.dataVersion]:
     * setelah menandai TTD, data diambil ulang.
     */
    fun tracker(month: YearMonth): Flow<TtdTrackerData> = RemoteSource(events) { loadTracker(month) }.flow

    private suspend fun loadTracker(month: YearMonth): Result<TtdTrackerData> = coroutineScope {
        val today = LocalDate.now()
        val current = YearMonth.from(today)
        // Bulan ini + bulan lalu selalu dimuat: cukup untuk riwayat 7 hari dan grafik 4 minggu.
        val months = linkedSetOf(month, current, current.minusMonths(1))
        val calls = months.associateWith { m -> async { apiCall { api.ttdMonth(m.toString()) } } }
        val settingsCall = async { settings() }

        val results = calls.mapValues { it.value.await() }
        val selected = results.getValue(month).getOrElse { return@coroutineScope Result.failure(it) }
        val currentDto = results.getValue(current).getOrElse { return@coroutineScope Result.failure(it) }

        val settings = settingsCall.await().getOrNull()
        val reminderTime = settings?.time ?: alarm.reminderTime
        val reminderOn = settings?.on ?: alarm.reminderOn
        val records = results.values.mapNotNull { it.getOrNull() }.flatMap { it.toDays() }.associateBy { it.date }

        fun dayOf(date: LocalDate) = records[date] ?: TtdDay(date, TtdStatus.BELUM_DIKONFIRMASI, reminderTime)

        val last7 = (0L until 7L).map { dayOf(today.minusDays(it)) }
        val weeks = (3 downTo 0).map { w ->
            val end = today.minusDays(7L * w)
            val start = end.minusDays(6)
            val taken = (0L until 7L).count { records[start.plusDays(it)]?.status == TtdStatus.SUDAH }
            TtdWeek(start, end, taken, Math.round(taken * 100f / 7))
        }
        val summary = TtdSummary(currentDto.takenLast7Days, currentDto.percentLast7Days, currentDto.takenToday)

        // Samakan alarm di HP dengan server.
        if (alarm.isEnabled && (alarm.reminderTime != reminderTime || alarm.reminderOn != reminderOn)) {
            alarm.update(reminderTime, reminderOn)
        }
        if (summary.takenToday && !alarm.isTakenToday()) alarm.markTaken(today)

        val selectedDays = selected.toDays()
        Result.success(
            TtdTrackerData(
                month = month,
                takenDates = selectedDays.filter { it.status == TtdStatus.SUDAH }.map { it.date }.toSet(),
                skippedDates = selectedDays.filter { it.status == TtdStatus.BELUM }.map { it.date }.toSet(),
                today = last7.first(),
                last7Days = last7,
                weeks = weeks,
                summary = summary,
                reminderTime = reminderTime,
                reminderOn = reminderOn
            )
        )
    }

    /** Ringkasan 7 hari terakhir (dipakai layar alarm). */
    suspend fun summary(): Result<TtdSummary> =
        apiCall { api.ttdMonth(YearMonth.now().toString()) }
            .map { TtdSummary(it.takenLast7Days, it.percentLast7Days, it.takenToday) }

    /** Tandai sudah minum hari ini (server memakai tanggal/jam saat ini). Alarm hari ini dihentikan. */
    suspend fun markTakenToday(): Result<Unit> =
        apiCall { api.ttdTake() }
            .map {
                alarm.markTaken()
                events.invalidateData()
            }
            .onFailure { events.showMessage(it.message ?: "Gagal menyimpan catatan TTD.") }

    /** Laporan "Belum diminum" dari alarm. Tidak menambah kepatuhan. */
    suspend fun markNotTakenToday(): Result<Unit> =
        apiCall { api.ttdSkip() }.map { events.invalidateData() }

    /** Pengaturan pengingat dari server (jam, nyala/mati, boleh dimatikan atau tidak). */
    suspend fun settings(): Result<TtdReminderSettings> =
        apiCall { api.ttdSettings() }.map { it.toSettings() }

    /** Ubah jam alarm harian di server, lalu pasang ulang alarm di HP. */
    suspend fun setReminderTime(time: LocalTime): Result<TtdReminderSettings> =
        apiCall {
            api.updateTtdSettings(TtdSettingsRequest(reminderTime = "%02d:%02d".format(time.hour, time.minute)))
        }.map { dto ->
            dto.toSettings().also {
                alarm.update(it.time, it.on)
                events.invalidateData()
            }
        }

    /**
     * Nyalakan/matikan pengingat. Server menolak mematikan bila Hb terakhir di bawah normal
     * (pesan galatnya siap ditampilkan).
     */
    suspend fun setReminderOn(on: Boolean): Result<TtdReminderSettings> =
        apiCall { api.updateTtdSettings(TtdSettingsRequest(reminderOn = on)) }
            .map { dto ->
                dto.toSettings().also {
                    alarm.update(it.time, it.on)
                    events.invalidateData()
                }
            }

    /** Pasang alarm dari pengaturan server; bila offline, pakai yang tersimpan di HP. */
    suspend fun syncAlarm() {
        settings()
            .onSuccess { alarm.update(it.time, it.on) }
            .onFailure { if (alarm.isEnabled) alarm.restore() else alarm.update(alarm.reminderTime) }
    }
}

private fun parseTime(value: String): LocalTime? = runCatching { LocalTime.parse(value) }.getOrNull()

private fun com.kelompok.anemom.data.remote.dto.TtdSettingsDto.toSettings() = TtdReminderSettings(
    time = parseTime(reminderTime) ?: TtdAlarmScheduler.DEFAULT_TIME,
    on = reminderOn,
    canDisable = canDisable
)

private fun TtdMonthDto.toDays(): List<TtdDay> =
    days.mapNotNull { it.toDay(TtdStatus.SUDAH) } + skipped.mapNotNull { it.toDay(TtdStatus.BELUM) }

private fun TtdDayDto.toDay(status: TtdStatus): TtdDay? {
    val d = runCatching { LocalDate.parse(date) }.getOrNull() ?: return null
    return TtdDay(d, status, parseTime(time) ?: LocalTime.of(19, 0))
}

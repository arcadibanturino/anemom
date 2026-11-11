package com.kelompok.anemom.data.repository

import com.kelompok.anemom.data.local.HealthStore
import com.kelompok.anemom.data.model.TtdEntry
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.LocalTime

/**
 * Repository catatan minum Tablet Tambah Darah (TTD).
 *
 * TODO(fitur): pengingat sungguhan (notifikasi terjadwal / Firebase Cloud Messaging);
 * saat ini hanya status aktif/tidaknya yang disimpan.
 */
class TtdRepository(private val store: HealthStore) {

    val entries: Flow<List<TtdEntry>> = store.ttdEntries
    val reminderOn: Flow<Boolean> = store.ttdReminderOn

    suspend fun markTakenToday() {
        val now = LocalTime.now().withSecond(0).withNano(0)
        store.markTtd(TtdEntry(date = LocalDate.now(), time = now))
    }

    suspend fun setReminder(on: Boolean) = store.setTtdReminder(on)
}

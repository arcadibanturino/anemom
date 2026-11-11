package com.kelompok.anemom.data.repository

import com.kelompok.anemom.data.local.HealthStore
import com.kelompok.anemom.data.model.HbResult
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/**
 * Repository hasil Hb darah (urut dari tanggal terlama ke terbaru).
 *
 * TODO(backend): ambil/kirim hasil Hb ke API.
 * TODO(fitur): estimasi AI (skor mata / PPG) disimpan terpisah dari hasil lab.
 */
class HbRepository(private val store: HealthStore) {

    val results: Flow<List<HbResult>> = store.hbResults

    suspend fun add(value: Float, date: LocalDate, facility: String, source: String) {
        store.addHbResult(HbResult(date = date, value = value, facility = facility.trim(), source = source))
    }
}

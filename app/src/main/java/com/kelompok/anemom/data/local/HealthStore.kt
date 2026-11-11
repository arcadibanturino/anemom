package com.kelompok.anemom.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.kelompok.anemom.data.model.HbResult
import com.kelompok.anemom.data.model.TtdEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

// Nama berbeda dari DataStore sesi login ("anemom_prefs").
private val Context.healthDataStore: DataStore<Preferences> by preferencesDataStore(name = "anemom_health")

/**
 * Penyimpanan lokal data kesehatan: hasil Hb, catatan minum TTD, dan pengaturan pengingat.
 *
 * TODO(backend): saat backend siap, data ini disinkronkan ke server;
 * bila datanya sudah banyak, pertimbangkan pindah ke Room.
 */
class HealthStore(context: Context) {

    private val dataStore = context.healthDataStore

    private object Keys {
        val HB_RESULTS = stringPreferencesKey("hb_results")
        val TTD_ENTRIES = stringSetPreferencesKey("ttd_entries")
        val TTD_REMINDER = booleanPreferencesKey("ttd_reminder_on")
    }

    private val preferences: Flow<Preferences> = dataStore.data.catch { e ->
        if (e is IOException) emit(emptyPreferences()) else throw e
    }

    val hbResults: Flow<List<HbResult>> =
        preferences.map { HealthCodec.decodeHb(it[Keys.HB_RESULTS].orEmpty()) }

    val ttdEntries: Flow<List<TtdEntry>> =
        preferences.map { p ->
            p[Keys.TTD_ENTRIES].orEmpty().mapNotNull { HealthCodec.decodeTtd(it) }
        }

    val ttdReminderOn: Flow<Boolean> = preferences.map { it[Keys.TTD_REMINDER] ?: true }

    suspend fun addHbResult(result: HbResult) {
        dataStore.edit { p ->
            val current = HealthCodec.decodeHb(p[Keys.HB_RESULTS].orEmpty())
            p[Keys.HB_RESULTS] = HealthCodec.encodeHb(current + result)
        }
    }

    /** Menyimpan catatan minum TTD. Jika tanggalnya sudah tercatat, tidak ditambah lagi. */
    suspend fun markTtd(entry: TtdEntry) {
        dataStore.edit { p ->
            val current = p[Keys.TTD_ENTRIES].orEmpty()
            val alreadyTaken = current.any { HealthCodec.decodeTtd(it)?.date == entry.date }
            if (!alreadyTaken) {
                p[Keys.TTD_ENTRIES] = current + HealthCodec.encodeTtd(entry)
            }
        }
    }

    suspend fun setTtdReminder(on: Boolean) {
        dataStore.edit { it[Keys.TTD_REMINDER] = on }
    }
}

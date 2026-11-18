package com.kelompok.anemom.data.repository

import com.kelompok.anemom.data.AppEvents
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow

/**
 * Data dari server sebagai Flow.
 *
 * [flow] mengambil data setiap kali ada yang mulai berlangganan dan setiap kali
 * [AppEvents.invalidateData] dipanggil. Jika pengambilan gagal, [flow] tidak mengeluarkan
 * apa pun (data lama di layar tetap), [error] terisi, dan pesannya ditampilkan sebagai Toast.
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal class RemoteSource<T>(
    private val events: AppEvents,
    private val fetch: suspend () -> Result<T>
) {
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    val flow: Flow<T> = events.dataVersion.flatMapLatest {
        flow {
            fetch()
                .onSuccess {
                    _error.value = null
                    emit(it)
                }
                .onFailure { e ->
                    val message = e.message ?: "Gagal memuat data."
                    _error.value = message
                    events.showMessage(message)
                }
        }
    }
}

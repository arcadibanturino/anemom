package com.kelompok.anemom.data.repository

import com.kelompok.anemom.data.remote.ApiService
import com.kelompok.anemom.data.remote.apiCall
import com.kelompok.anemom.data.remote.dto.ConsentRequest
import java.time.Instant

/**
 * Persetujuan "Privasi & disclaimer" (tabel consents). Menyetujui = privacy_policy + health_data
 * untuk [VERSION]. Naikkan VERSION bila isi layar berubah supaya Bunda diminta menyetujui ulang.
 */
class ConsentRepository(private val api: ApiService) {

    /** Waktu persetujuan versi sekarang; null = belum. */
    suspend fun acceptedAt(): Result<Instant?> =
        apiCall { api.consents() }.map { list ->
            list.filter { it.version == VERSION && it.revokedAt == null && it.type in TYPES }
                .takeIf { accepted -> TYPES.all { t -> accepted.any { it.type == t } } }
                ?.mapNotNull { runCatching { Instant.parse(it.acceptedAt) }.getOrNull() }
                ?.maxOrNull()
        }

    suspend fun accept(): Result<Unit> = runCatching {
        TYPES.forEach { type -> apiCall { api.setConsent(ConsentRequest(type, VERSION)) }.getOrThrow() }
    }

    companion object {
        const val VERSION = "2026-11"
        private val TYPES = listOf("privacy_policy", "health_data")
    }
}

package com.kelompok.anemom.data.repository

import com.kelompok.anemom.data.model.AppNotification
import com.kelompok.anemom.data.model.NotificationType
import com.kelompok.anemom.data.remote.ApiEnvelope
import com.kelompok.anemom.data.remote.ApiException
import com.kelompok.anemom.data.remote.ApiService
import com.kelompok.anemom.data.remote.apiCall
import com.kelompok.anemom.data.remote.dto.NotificationDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.time.Instant

/**
 * Notifikasi dalam aplikasi. Server membuat pengingat (TTD, periksa Hb) saat daftar atau jumlahnya diminta.
 * [unreadCount] dipakai bersama Beranda (titik merah) dan layar Notifikasi, jadi tanda baca langsung terlihat.
 */
class NotificationRepository(private val api: ApiService) {

    private val _unreadCount = MutableStateFlow(0)
    val unreadCount: StateFlow<Int> = _unreadCount.asStateFlow()

    /** Perbarui jumlah belum dibaca. Gagal = diam saja (titik merah tetap seperti sebelumnya). */
    suspend fun refreshUnread() {
        apiCall { api.notificationUnreadCount() }.onSuccess { _unreadCount.value = it.unread }
    }

    suspend fun notifications(): Result<List<AppNotification>> =
        apiCall {
            val envelope = api.notifications()
            val items = envelope.data.orEmpty().map { it.toModel() }
            val unread = runCatching { envelope.meta?.asJsonObject?.get("unread")?.asInt }.getOrNull()
            _unreadCount.value = unread ?: items.count { !it.isRead }
            ApiEnvelope(items)
        }

    suspend fun markRead(id: Long): Result<Unit> {
        val result = apiCall { api.markNotificationRead(id) }
        // 404 = sudah dibaca sebelumnya; tetap dianggap berhasil.
        val ok = result.isSuccess || (result.exceptionOrNull() as? ApiException)?.httpStatus == 404
        if (ok) _unreadCount.update { (it - 1).coerceAtLeast(0) }
        return if (ok) Result.success(Unit) else Result.failure(result.exceptionOrNull()!!)
    }

    suspend fun markAllRead(): Result<Unit> =
        apiCall { api.markAllNotificationsRead() }
            .map { }
            .onSuccess { _unreadCount.value = 0 }
}

private fun NotificationDto.toModel() = AppNotification(
    id = id,
    type = NotificationType.fromCode(type),
    title = title,
    body = body,
    time = scheduledAt?.let { runCatching { Instant.parse(it) }.getOrNull() },
    isRead = readAt != null
)

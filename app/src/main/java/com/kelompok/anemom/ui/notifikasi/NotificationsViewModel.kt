package com.kelompok.anemom.ui.notifikasi

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kelompok.anemom.data.model.AppNotification
import com.kelompok.anemom.data.repository.NotificationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NotificationsUiState(
    val isLoading: Boolean = true,
    val items: List<AppNotification> = emptyList(),
    val error: String? = null,
    val isMarkingAll: Boolean = false,
    val message: String? = null
) {
    val unread: Int get() = items.count { !it.isRead }
}

class NotificationsViewModel(private val repository: NotificationRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(NotificationsUiState())
    val uiState: StateFlow<NotificationsUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            repository.notifications()
                .onSuccess { list -> _uiState.update { it.copy(isLoading = false, items = list) } }
                .onFailure { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message ?: "Notifikasi gagal dimuat.") }
                }
        }
    }

    /** Ditandai dibaca langsung di layar; bila server gagal, tanda dikembalikan. */
    fun markRead(item: AppNotification) {
        if (item.isRead) return
        setRead(item.id, true)
        viewModelScope.launch {
            repository.markRead(item.id).onFailure { setRead(item.id, false) }
        }
    }

    fun markAllRead() {
        if (_uiState.value.isMarkingAll || _uiState.value.unread == 0) return
        _uiState.update { it.copy(isMarkingAll = true) }
        viewModelScope.launch {
            repository.markAllRead()
                .onSuccess {
                    _uiState.update { s -> s.copy(isMarkingAll = false, items = s.items.map { it.copy(isRead = true) }) }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isMarkingAll = false, message = e.message ?: "Gagal menandai notifikasi.") }
                }
        }
    }

    fun messageShown() {
        _uiState.update { it.copy(message = null) }
    }

    private fun setRead(id: Long, read: Boolean) {
        _uiState.update { s -> s.copy(items = s.items.map { if (it.id == id) it.copy(isRead = read) else it }) }
    }
}

package com.kelompok.anemom.ui.pengingat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kelompok.anemom.alarm.AlarmTimeText
import com.kelompok.anemom.alarm.TtdAlarmScheduler
import com.kelompok.anemom.data.repository.TtdRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.time.ZonedDateTime

data class ReminderUiState(
    val reminderTime: LocalTime,
    val reminderOn: Boolean = true,
    /** false = Hb lab terakhir di bawah normal: sakelar dikunci menyala. */
    val canDisable: Boolean = true,
    val isToggling: Boolean = false,
    val nextAlarm: ZonedDateTime? = null,
    val isSaving: Boolean = false,
    /** Sekali tampil sebagai Toast. */
    val message: String? = null,
    /** true setelah jam tersimpan (layar Ubah jam kembali). */
    val saved: Boolean = false
)

/** Dipakai layar Pengaturan pengingat dan Ubah jam alarm. */
class ReminderViewModel(
    private val repository: TtdRepository,
    private val alarm: TtdAlarmScheduler
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        ReminderUiState(reminderTime = alarm.reminderTime, reminderOn = alarm.reminderOn, nextAlarm = alarm.nextTrigger())
    )
    val uiState: StateFlow<ReminderUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.settings().onSuccess { s ->
                if (alarm.reminderTime != s.time || alarm.reminderOn != s.on || !alarm.isEnabled) alarm.update(s.time, s.on)
                _uiState.update {
                    it.copy(reminderTime = s.time, reminderOn = s.on, canDisable = s.canDisable, nextAlarm = alarm.nextTrigger())
                }
            }
        }
    }

    fun refresh() {
        _uiState.update { it.copy(reminderTime = alarm.reminderTime, reminderOn = alarm.reminderOn, nextAlarm = alarm.nextTrigger()) }
    }

    /** Sakelar pengingat. Ditolak server bila Hb terakhir di bawah normal. */
    fun setReminderOn(on: Boolean) {
        val s = _uiState.value
        if (s.isToggling || s.reminderOn == on) return
        if (!on && !s.canDisable) {
            _uiState.update { it.copy(message = "Hb terakhir Bunda di bawah normal, jadi pengingat TTD wajib aktif.") }
            return
        }
        _uiState.update { it.copy(isToggling = true, reminderOn = on) }
        viewModelScope.launch {
            repository.setReminderOn(on)
                .onSuccess { r ->
                    _uiState.update {
                        it.copy(
                            isToggling = false,
                            reminderOn = r.on,
                            canDisable = r.canDisable,
                            nextAlarm = alarm.nextTrigger(),
                            message = if (r.on) "Pengingat TTD dinyalakan." else "Pengingat TTD dimatikan."
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(
                            isToggling = false,
                            reminderOn = !on,
                            canDisable = if ((e as? com.kelompok.anemom.data.remote.ApiException)?.code == "reminder_locked") false else it.canDisable,
                            message = e.message ?: "Gagal mengubah pengingat."
                        )
                    }
                }
        }
    }

    fun saveTime(time: LocalTime) {
        if (_uiState.value.isSaving) return
        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            repository.setReminderTime(time)
                .onSuccess { r ->
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            saved = true,
                            reminderTime = r.time,
                            reminderOn = r.on,
                            canDisable = r.canDisable,
                            nextAlarm = alarm.nextTrigger(),
                            message = "Alarm TTD diatur pukul ${AlarmTimeText.timeWithZone(time)}."
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isSaving = false, message = e.message ?: "Gagal menyimpan jam alarm.") }
                }
        }
    }

    /** Alarm uji 1 menit lagi: untuk memastikan alarm muncul saat aplikasi ditutup dan layar terkunci. */
    fun scheduleTest() {
        val at = alarm.scheduleTest(60)
        _uiState.update {
            it.copy(
                message = "Alarm uji berbunyi pukul ${AlarmTimeText.time(at.toLocalTime())}. " +
                    "Kunci layar atau tutup aplikasi untuk mencobanya."
            )
        }
    }

    fun messageShown() {
        _uiState.update { it.copy(message = null) }
    }
}

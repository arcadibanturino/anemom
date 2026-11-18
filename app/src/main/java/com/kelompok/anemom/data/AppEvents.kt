package com.kelompok.anemom.data

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Dua saluran kecil yang dipakai bersama oleh semua repository:
 *
 * - [dataVersion]: naik setiap ada data yang berubah di server (Hb baru, TTD ditandai, dst.).
 *   Semua layar yang sedang tampil mengambil ulang datanya dari server.
 * - [messages]: pesan singkat (mis. galat jaringan) yang ditampilkan sebagai Toast oleh MainActivity.
 */
class AppEvents {

    private val _dataVersion = MutableStateFlow(0)
    val dataVersion: StateFlow<Int> = _dataVersion.asStateFlow()

    private val _messages = MutableSharedFlow<String>(
        extraBufferCapacity = 8,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    private var lastMessage = ""
    private var lastMessageAt = 0L

    fun invalidateData() {
        _dataVersion.update { it + 1 }
    }

    /** Pesan yang sama dalam 3 detik diabaikan, supaya beberapa layar yang gagal bersamaan tidak membanjiri Toast. */
    fun showMessage(text: String) {
        val now = System.currentTimeMillis()
        if (text == lastMessage && now - lastMessageAt < 3_000) return
        lastMessage = text
        lastMessageAt = now
        _messages.tryEmit(text)
    }
}

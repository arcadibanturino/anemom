package com.kelompok.anemom.ui.navigation

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Tujuan yang diminta dari luar navigasi (mis. layar alarm -> "Lihat Pelacak TTD").
 * MainActivity mengisinya dari Intent; MainScreen membuka tab lalu mengosongkannya.
 */
object PendingNavigation {
    const val TTD = "ttd"

    private val _target = MutableStateFlow<String?>(null)
    val target: StateFlow<String?> = _target.asStateFlow()

    fun open(target: String) {
        _target.value = target
    }

    fun consume() {
        _target.value = null
    }
}

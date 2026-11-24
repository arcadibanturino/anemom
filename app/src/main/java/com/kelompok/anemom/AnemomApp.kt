package com.kelompok.anemom

import android.app.Application
import com.kelompok.anemom.alarm.AlarmNotifier
import com.kelompok.anemom.data.AppContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class AnemomApp : Application() {

    /** Penyedia dependensi manual (tanpa Hilt) agar proyek tetap sederhana. */
    lateinit var container: AppContainer
        private set

    /** Untuk pekerjaan latar yang tidak terikat layar (mis. menyamakan alarm TTD). */
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        AlarmNotifier.ensureChannel(this)

        // Login -> pasang alarm TTD dari jam di server. Logout / sesi berakhir -> alarm dibatalkan.
        appScope.launch {
            container.sessionManager.token
                .map { it != null }
                .distinctUntilChanged()
                .collect { loggedIn ->
                    if (loggedIn) container.ttdRepository.syncAlarm() else container.ttdAlarm.disable()
                }
        }
    }
}

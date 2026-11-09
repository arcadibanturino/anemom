package com.kelompok.anemom

import android.app.Application
import com.kelompok.anemom.data.AppContainer

class AnemomApp : Application() {

    /** Penyedia dependensi manual (tanpa Hilt) agar proyek tetap sederhana. */
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

package com.kelompok.anemom.ui.navigation

import android.net.Uri

object Routes {
    const val SPLASH = "splash"
    const val ONBOARDING = "onboarding"
    const val LOGIN = "login"
    const val REGISTER = "register"

    /** Langkah 2 pendaftaran: Profil Kesehatan Awal. Wajib diisi sebelum Main (dashboard). */
    const val HEALTH_PROFILE = "health_profile"

    const val FORGOT_PASSWORD = "forgot_password"
    const val MAIN = "main"

    const val MENU_LIST = "menu_list"

    const val TODO_ARG_TITLE = "title"
    const val TODO = "todo/{$TODO_ARG_TITLE}"

    fun todo(title: String): String = "todo/${Uri.encode(title)}"
}

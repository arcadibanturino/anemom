package com.kelompok.anemom.ui.navigation

import android.net.Uri

object Routes {
    const val SPLASH = "splash"
    const val ONBOARDING = "onboarding"
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val MAIN = "main"

    const val TODO_ARG_TITLE = "title"
    const val TODO = "todo/{$TODO_ARG_TITLE}"

    fun todo(title: String): String = "todo/${Uri.encode(title)}"
}

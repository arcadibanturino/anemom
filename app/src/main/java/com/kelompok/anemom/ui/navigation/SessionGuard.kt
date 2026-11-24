package com.kelompok.anemom.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.kelompok.anemom.AnemomApp
import kotlinx.coroutines.delay

/** Halaman yang boleh dibuka tanpa sesi login. */
private val publicRoutes = setOf(
    Routes.SPLASH,
    Routes.ONBOARDING,
    Routes.LOGIN,
    Routes.REGISTER,
    Routes.FORGOT_PASSWORD
)

/**
 * Menjaga sesi: bila token hilang saat pengguna sedang di halaman yang butuh login
 * (mis. server menolak token yang kedaluwarsa/dicabut), pengguna dikembalikan ke Masuk.
 *
 * Guard kedua: bila profil kesehatan belum lengkap, halaman di balik login diganti Profil Kesehatan Awal.
 *
 * Keluar lewat tombol Logout di Profil sudah menavigasi sendiri; jeda singkat di bawah
 * membuat guard melihat halaman Masuk yang sudah terbuka dan tidak menumpuk dua kali.
 */
@Composable
fun SessionGuard(navController: NavHostController) {
    val context = LocalContext.current
    val authRepository = remember { (context.applicationContext as AnemomApp).container.authRepository }
    val loggedIn by authRepository.isLoggedIn.collectAsStateWithLifecycle(initialValue = true)
    val profileCompleted by authRepository.profileCompleted.collectAsStateWithLifecycle(initialValue = true)
    val currentEntry by navController.currentBackStackEntryFlow.collectAsStateWithLifecycle(initialValue = null)

    LaunchedEffect(loggedIn) {
        if (loggedIn) return@LaunchedEffect
        delay(300)
        val route = navController.currentBackStackEntry?.destination?.route
        if (route != null && route !in publicRoutes) {
            navController.navigate(Routes.LOGIN) {
                popUpTo(0) { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    // Akun yang Profil Kesehatan Awal-nya belum diisi tidak boleh membuka halaman di balik login
    // (mis. akun lama yang baru ketahuan belum lengkap setelah profil disegarkan dari server).
    val currentRoute = currentEntry?.destination?.route
    LaunchedEffect(loggedIn, profileCompleted, currentRoute) {
        if (!loggedIn || profileCompleted) return@LaunchedEffect
        if (currentRoute != null && currentRoute !in publicRoutes && currentRoute != Routes.HEALTH_PROFILE) {
            navController.navigate(Routes.HEALTH_PROFILE) {
                popUpTo(0) { inclusive = true }
                launchSingleTop = true
            }
        }
    }
}

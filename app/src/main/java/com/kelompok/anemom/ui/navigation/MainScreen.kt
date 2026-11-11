package com.kelompok.anemom.ui.navigation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CenterFocusStrong
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Medication
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.kelompok.anemom.ui.about.AboutScreen
import com.kelompok.anemom.ui.home.HomeScreen
import com.kelompok.anemom.ui.profile.ProfileScreen
import com.kelompok.anemom.ui.riwayat.RiwayatScreen
import com.kelompok.anemom.ui.skrining.AiScanScreen
import com.kelompok.anemom.ui.skrining.InputHbScreen
import com.kelompok.anemom.ui.skrining.SkriningScreen
import com.kelompok.anemom.ui.skrining.SmartwatchScreen
import com.kelompok.anemom.ui.ttd.TtdScreen
import com.kelompok.anemom.ui.theme.MutedText
import com.kelompok.anemom.ui.theme.Pink100
import com.kelompok.anemom.ui.theme.Pink500

/** Tab pada bottom navigation (urutan dan nama mengikuti desain Figma). */
enum class MainTab(val route: String, val label: String, val icon: ImageVector) {
    Beranda("beranda", "Beranda", Icons.Outlined.Home),
    Ttd("ttd", "TTD", Icons.Outlined.Medication),
    Skrining("skrining", "Skrining", Icons.Outlined.CenterFocusStrong),
    Riwayat("riwayat", "Riwayat", Icons.Outlined.History),
    Profil("profil", "Profil", Icons.Outlined.Person)
}

/** Halaman "Tentang Anemom" berada di dalam tab Profil, jadi bottom nav tetap tampil. */
private const val ROUTE_ABOUT = "tentang"

/** Sub-halaman Skrining (bottom nav tetap tampil, tab Skrining tetap menyala). */
private const val ROUTE_AI_SCAN = "ai_scan"
private const val ROUTE_SMARTWATCH = "smartwatch"
private const val ROUTE_INPUT_HB = "input_hb"
private val skriningSubRoutes = setOf(ROUTE_AI_SCAN, ROUTE_SMARTWATCH, ROUTE_INPUT_HB)

/** Tab yang menyala untuk sebuah route. */
private fun tabFor(route: String?): MainTab? = when {
    route == null -> null
    route == ROUTE_ABOUT -> MainTab.Profil
    route in skriningSubRoutes -> MainTab.Skrining
    else -> MainTab.entries.firstOrNull { it.route == route }
}

@Composable
fun MainScreen(
    onOpenFeature: (featureName: String) -> Unit,
    onLoggedOut: () -> Unit
) {
    val tabNavController = rememberNavController()
    val backStackEntry by tabNavController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    Scaffold(
        containerColor = Color.Transparent,
        // Setiap halaman mengatur sendiri inset status bar-nya.
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            NavigationBar(containerColor = Color.White) {
                MainTab.entries.forEach { tab ->
                    val selected = tabFor(currentRoute) == tab
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            if (currentRoute == ROUTE_ABOUT && tab == MainTab.Profil) {
                                tabNavController.popBackStack()
                            } else if (currentRoute in skriningSubRoutes && tab == MainTab.Skrining) {
                                tabNavController.popBackStack(MainTab.Skrining.route, inclusive = false)
                            } else {
                                tabNavController.navigateToTab(tab)
                            }
                        },
                        icon = { Icon(imageVector = tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Pink500,
                            selectedTextColor = Pink500,
                            indicatorColor = Pink100,
                            unselectedIconColor = MutedText,
                            unselectedTextColor = MutedText
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = tabNavController,
            startDestination = MainTab.Beranda.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(MainTab.Beranda.route) {
                HomeScreen(
                    onFeatureClick = { name ->
                        when (name) {
                            "AI Scan Mata" -> tabNavController.navigate(ROUTE_AI_SCAN)
                            "Smartwatch" -> tabNavController.navigate(ROUTE_SMARTWATCH)
                            "Grafik Hb" -> tabNavController.navigateToTab(MainTab.Riwayat)
                            else -> onOpenFeature(name)
                        }
                    }
                )
            }
            composable(MainTab.Ttd.route) {
                TtdScreen(onBack = { tabNavController.navigateToTab(MainTab.Beranda) })
            }
            composable(MainTab.Skrining.route) {
                SkriningScreen(
                    onBack = { tabNavController.navigateToTab(MainTab.Beranda) },
                    onAiScan = { tabNavController.navigate(ROUTE_AI_SCAN) },
                    onSmartwatch = { tabNavController.navigate(ROUTE_SMARTWATCH) },
                    onInputHb = { tabNavController.navigate(ROUTE_INPUT_HB) }
                )
            }
            composable(MainTab.Riwayat.route) {
                RiwayatScreen(
                    onBack = { tabNavController.navigateToTab(MainTab.Beranda) },
                    onAddHb = { tabNavController.navigate(ROUTE_INPUT_HB) }
                )
            }
            composable(ROUTE_AI_SCAN) {
                AiScanScreen(
                    onBack = { tabNavController.popBackStack() },
                    onInputHb = { tabNavController.navigate(ROUTE_INPUT_HB) }
                )
            }
            composable(ROUTE_SMARTWATCH) {
                SmartwatchScreen(
                    onBack = { tabNavController.popBackStack() },
                    onInputHb = { tabNavController.navigate(ROUTE_INPUT_HB) }
                )
            }
            composable(ROUTE_INPUT_HB) {
                InputHbScreen(
                    onBack = { tabNavController.popBackStack() },
                    onSaved = { tabNavController.navigateToTab(MainTab.Riwayat) }
                )
            }
            composable(MainTab.Profil.route) {
                ProfileScreen(
                    onBack = { tabNavController.navigateToTab(MainTab.Beranda) },
                    onOpenAbout = { tabNavController.navigate(ROUTE_ABOUT) },
                    onOpenFeature = onOpenFeature,
                    onLoggedOut = onLoggedOut
                )
            }
            composable(ROUTE_ABOUT) {
                AboutScreen(onBack = { tabNavController.popBackStack() })
            }
        }
    }
}

private fun NavController.navigateToTab(tab: MainTab) {
    navigate(tab.route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

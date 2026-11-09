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
import com.kelompok.anemom.ui.placeholder.FeaturePlaceholder
import com.kelompok.anemom.ui.profile.ProfileScreen
import com.kelompok.anemom.ui.theme.MutedText
import com.kelompok.anemom.ui.theme.Pink100
import com.kelompok.anemom.ui.theme.Pink500

/** Tab pada bottom navigation (urutan dan nama mengikuti desain Figma). */
enum class MainTab(val route: String, val label: String, val icon: ImageVector) {
    Beranda("beranda", "Beranda", Icons.Outlined.Home),
    Skrining("skrining", "Skrining", Icons.Outlined.CenterFocusStrong),
    Ttd("ttd", "TTD", Icons.Outlined.Medication),
    Riwayat("riwayat", "Riwayat", Icons.Outlined.History),
    Profil("profil", "Profil", Icons.Outlined.Person)
}

/** Halaman "Tentang Anemom" berada di dalam tab Profil, jadi bottom nav tetap tampil. */
private const val ROUTE_ABOUT = "tentang"

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
                    val selected = currentRoute == tab.route ||
                        (tab == MainTab.Profil && currentRoute == ROUTE_ABOUT)
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            if (tab == MainTab.Profil && currentRoute == ROUTE_ABOUT) {
                                tabNavController.popBackStack()
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
                HomeScreen(onFeatureClick = onOpenFeature)
            }
            // TODO(fitur): ganti placeholder berikut dengan fitur sungguhan.
            composable(MainTab.Skrining.route) { FeaturePlaceholder(title = "Skrining") }
            composable(MainTab.Ttd.route) { FeaturePlaceholder(title = "TTD") }
            composable(MainTab.Riwayat.route) { FeaturePlaceholder(title = "Riwayat") }
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

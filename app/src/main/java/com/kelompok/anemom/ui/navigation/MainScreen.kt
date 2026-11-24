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
import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
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
import com.kelompok.anemom.ui.ftux.HomeTourController
import com.kelompok.anemom.ui.ftux.HomeTourOverlay
import com.kelompok.anemom.ui.ftux.LocalHomeTour
import com.kelompok.anemom.ui.ftux.TourTarget
import com.kelompok.anemom.ui.ftux.tourTarget
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import com.kelompok.anemom.ui.notifikasi.NotificationsScreen
import com.kelompok.anemom.ui.pengingat.AlarmTimeScreen
import com.kelompok.anemom.ui.pengingat.ReminderSettingsScreen
import com.kelompok.anemom.ui.profile.EditProfileScreen
import com.kelompok.anemom.ui.profile.ProfileScreen
import com.kelompok.anemom.ui.riwayat.RiwayatScreen
import com.kelompok.anemom.ui.riwayat.HbChartScreen
import com.kelompok.anemom.ui.riwayat.HistoryDetailScreen
import com.kelompok.anemom.ui.riwayat.HISTORY_ARG_ID
import com.kelompok.anemom.ui.riwayat.HISTORY_ARG_KIND
import com.kelompok.anemom.ui.privasi.PrivacyScreen
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.kelompok.anemom.ui.skrining.AiScanScreen
import com.kelompok.anemom.ui.skrining.InputHbScreen
import com.kelompok.anemom.ui.skrining.SkriningScreen
import com.kelompok.anemom.ui.skrining.smartband.PpgRecordScreen
import com.kelompok.anemom.ui.skrining.smartband.SmartbandConnectScreen
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

/** Halaman "Edit profil" juga di dalam tab Profil. */
private const val ROUTE_EDIT_PROFILE = "edit_profil"

/** Pengaturan pengingat (dari Profil) dan Ubah jam alarm (dari Pengaturan pengingat atau "Edit" di TTD). */
private const val ROUTE_REMINDER = "pengingat"
private const val ROUTE_ALARM_TIME = "jam_alarm"

/** Privasi & disclaimer (dari Profil). */
private const val ROUTE_PRIVACY = "privasi"

/** Grafik Hb darah (dari kartu Tren Hb di Beranda). */
private const val ROUTE_HB_CHART = "grafik_hb"

/** Detail riwayat (dari tab Riwayat). */
private const val ROUTE_HISTORY_DETAIL = "riwayat_detail/{$HISTORY_ARG_KIND}/{$HISTORY_ARG_ID}"
private fun historyDetail(kind: String, id: Long) = "riwayat_detail/$kind/$id"

/** Notifikasi dibuka dari ikon hati di Beranda (tab Beranda tetap menyala). */
private const val ROUTE_NOTIFICATIONS = "notifikasi"

/** Sub-halaman Skrining (bottom nav tetap tampil, tab Skrining tetap menyala). */
private const val ROUTE_AI_SCAN = "ai_scan"
/** Koneksi Bluetooth smartband (layar pertama Smartband / PPG). */
private const val ROUTE_Smartband = "Smartband"
/** Rekam PPG, dibuka dari Koneksi Bluetooth. */
private const val ROUTE_PPG_RECORD = "ppg_record"
private const val ROUTE_INPUT_HB = "input_hb"
private val skriningSubRoutes = setOf(ROUTE_AI_SCAN, ROUTE_Smartband, ROUTE_PPG_RECORD, ROUTE_INPUT_HB)

/** Tab yang menyala untuk sebuah route. */
private fun tabFor(route: String?): MainTab? = when {
    route == null -> null
    route == ROUTE_ABOUT || route == ROUTE_EDIT_PROFILE || route == ROUTE_REMINDER || route == ROUTE_PRIVACY -> MainTab.Profil
    route == ROUTE_NOTIFICATIONS || route == ROUTE_HB_CHART -> MainTab.Beranda
    route == ROUTE_HISTORY_DETAIL -> MainTab.Riwayat
    route in skriningSubRoutes -> MainTab.Skrining
    else -> MainTab.entries.firstOrNull { it.route == route }
}

@Composable
fun MainScreen(
    onOpenFeature: (featureName: String) -> Unit,
    onOpenMenuList: () -> Unit,
    onOpenFoodDetail: (foodId: Long) -> Unit,
    onLoggedOut: () -> Unit
) {
    val tabNavController = rememberNavController()
    val backStackEntry by tabNavController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    // Ubah jam alarm bisa dibuka dari tab TTD maupun Profil: tab yang menyala mengikuti layar sebelumnya.
    val highlightedRoute = if (currentRoute == ROUTE_ALARM_TIME) {
        tabNavController.previousBackStackEntry?.destination?.route
    } else {
        currentRoute
    }

    // Layar alarm -> "Lihat Pelacak TTD".
    LaunchedEffect(Unit) {
        PendingNavigation.target.collect { target ->
            if (target == PendingNavigation.TTD) {
                tabNavController.navigateToTab(MainTab.Ttd)
                PendingNavigation.consume()
            }
        }
    }

    // Alarm TTD butuh izin notifikasi (Android 13+). Ditanyakan sekali saat pertama masuk.
    val context = LocalContext.current
    var askedNotification by rememberSaveable { mutableStateOf(false) }
    val notificationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    LaunchedEffect(Unit) {
        if (!askedNotification && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            askedNotification = true
            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // Panduan pertama kali di Beranda: mulai setelah kartu Beranda tampil, sekali per HP.
    val tour = remember { HomeTourController(context) }
    val dashboardReady = tour.isReady(TourTarget.DASHBOARD)
    LaunchedEffect(currentRoute, dashboardReady) {
        if (currentRoute == MainTab.Beranda.route && dashboardReady) tour.start()
    }

    CompositionLocalProvider(LocalHomeTour provides tour) {
    Box(Modifier.fillMaxSize()) {
    Scaffold(
        containerColor = Color.Transparent,
        // Setiap halaman mengatur sendiri inset status bar-nya.
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            NavigationBar(containerColor = Color.White) {
                MainTab.entries.forEach { tab ->
                    val selected = tabFor(highlightedRoute) == tab
                    NavigationBarItem(
                        modifier = if (tab == MainTab.Skrining) Modifier.tourTarget(TourTarget.SKRINING) else Modifier,
                        selected = selected,
                        onClick = {
                            if (currentRoute == ROUTE_ALARM_TIME && tabFor(highlightedRoute) == tab) {
                                tabNavController.popBackStack()
                            } else if (currentRoute == ROUTE_HISTORY_DETAIL && tab == MainTab.Riwayat) {
                                tabNavController.popBackStack()
                            } else if ((currentRoute == ROUTE_ABOUT || currentRoute == ROUTE_EDIT_PROFILE || currentRoute == ROUTE_REMINDER || currentRoute == ROUTE_PRIVACY) && tab == MainTab.Profil) {
                                tabNavController.popBackStack()
                            } else if ((currentRoute == ROUTE_NOTIFICATIONS || currentRoute == ROUTE_HB_CHART) && tab == MainTab.Beranda) {
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
                            "Smartband" -> tabNavController.navigate(ROUTE_Smartband)
                            "Grafik Hb" -> tabNavController.navigate(ROUTE_HB_CHART)
                            "Rekomendasi Menu Makanan" -> onOpenMenuList()
                            else -> onOpenFeature(name)
                        }
                    },
                    onOpenNotifications = { tabNavController.navigate(ROUTE_NOTIFICATIONS) },
                    onOpenFoodDetail = onOpenFoodDetail,
                    onOpenHbChart = { tabNavController.navigate(ROUTE_HB_CHART) }
                )
            }
            composable(ROUTE_HB_CHART) {
                HbChartScreen(
                    onBack = { tabNavController.popBackStack() },
                    onAddHb = { tabNavController.navigate(ROUTE_INPUT_HB) }
                )
            }
            composable(
                route = ROUTE_HISTORY_DETAIL,
                arguments = listOf(
                    navArgument(HISTORY_ARG_KIND) { type = NavType.StringType },
                    navArgument(HISTORY_ARG_ID) { type = NavType.LongType }
                )
            ) {
                HistoryDetailScreen(onBack = { tabNavController.popBackStack() })
            }
            composable(ROUTE_PRIVACY) {
                PrivacyScreen(onBack = { tabNavController.popBackStack() })
            }
            composable(ROUTE_NOTIFICATIONS) {
                NotificationsScreen(
                    onBack = { tabNavController.popBackStack() },
                    onOpenTtd = { tabNavController.navigateToTab(MainTab.Ttd) },
                    onOpenInputHb = { tabNavController.navigate(ROUTE_INPUT_HB) }
                )
            }
            composable(MainTab.Ttd.route) {
                TtdScreen(
                    onBack = { tabNavController.navigateToTab(MainTab.Beranda) },
                    onEditAlarm = { tabNavController.navigate(ROUTE_ALARM_TIME) }
                )
            }
            composable(MainTab.Skrining.route) {
                SkriningScreen(
                    onBack = { tabNavController.navigateToTab(MainTab.Beranda) },
                    onAiScan = { tabNavController.navigate(ROUTE_AI_SCAN) },
                    onSmartband = { tabNavController.navigate(ROUTE_Smartband) },
                    onInputHb = { tabNavController.navigate(ROUTE_INPUT_HB) }
                )
            }
            composable(MainTab.Riwayat.route) {
                RiwayatScreen(
                    onBack = { tabNavController.navigateToTab(MainTab.Beranda) },
                    onAddHb = { tabNavController.navigate(ROUTE_INPUT_HB) },
                    onOpenDetail = { kind, id -> tabNavController.navigate(historyDetail(kind.route, id)) }
                )
            }
            composable(ROUTE_AI_SCAN) {
                AiScanScreen(
                    onBack = { tabNavController.popBackStack() },
                    onInputHb = { tabNavController.navigate(ROUTE_INPUT_HB) }
                )
            }
            composable(ROUTE_Smartband) {
                SmartbandConnectScreen(
                    onBack = { tabNavController.popBackStack() },
                    onRecordPpg = { tabNavController.navigate(ROUTE_PPG_RECORD) }
                )
            }
            composable(ROUTE_PPG_RECORD) {
                PpgRecordScreen(
                    onBack = { tabNavController.popBackStack() },
                    onManageConnection = {
                        // Kembali ke Koneksi Bluetooth; kalau tidak ada di tumpukan, buka baru.
                        if (!tabNavController.popBackStack(ROUTE_Smartband, inclusive = false)) {
                            tabNavController.navigate(ROUTE_Smartband)
                        }
                    }
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
                    onEditProfile = { tabNavController.navigate(ROUTE_EDIT_PROFILE) },
                    onOpenFeature = { name ->
                        when (name) {
                            "Pengaturan pengingat" -> tabNavController.navigate(ROUTE_REMINDER)
                            "Privasi & disclaimer" -> tabNavController.navigate(ROUTE_PRIVACY)
                            else -> onOpenFeature(name)
                        }
                    },
                    onLoggedOut = onLoggedOut
                )
            }
            composable(ROUTE_REMINDER) {
                ReminderSettingsScreen(
                    onBack = { tabNavController.popBackStack() },
                    onEditTime = { tabNavController.navigate(ROUTE_ALARM_TIME) }
                )
            }
            composable(ROUTE_ALARM_TIME) {
                AlarmTimeScreen(onBack = { tabNavController.popBackStack() })
            }
            composable(ROUTE_EDIT_PROFILE) {
                EditProfileScreen(
                    onBack = { tabNavController.popBackStack() },
                    onSaved = { tabNavController.popBackStack() }
                )
            }
            composable(ROUTE_ABOUT) {
                AboutScreen(onBack = { tabNavController.popBackStack() })
            }
        }
    }
    HomeTourOverlay(tour)
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

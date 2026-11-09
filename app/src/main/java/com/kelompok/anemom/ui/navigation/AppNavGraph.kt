package com.kelompok.anemom.ui.navigation

import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.kelompok.anemom.ui.auth.LoginScreen
import com.kelompok.anemom.ui.auth.RegisterScreen
import com.kelompok.anemom.ui.onboarding.OnboardingScreen
import com.kelompok.anemom.ui.placeholder.TodoScreen
import com.kelompok.anemom.ui.splash.SplashScreen

/**
 * Alur utama:
 * Splash -> (Onboarding ->) Login <-> Register -> Main (bottom nav) -> halaman fitur TODO.
 * Logout dari Profil mengembalikan ke Login.
 */
@Composable
fun AppNavGraph(navController: NavHostController = rememberNavController()) {
    val backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher

    NavHost(navController = navController, startDestination = Routes.SPLASH) {

        composable(Routes.SPLASH) {
            SplashScreen(
                onFinished = { nextRoute ->
                    navController.navigate(nextRoute) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.ONBOARDING) {
            OnboardingScreen(
                onFinished = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(Routes.ONBOARDING) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.LOGIN) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Routes.MAIN) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                },
                onNavigateToRegister = { navController.navigate(Routes.REGISTER) },
                // Tombol kembali di Login = perilaku back sistem (keluar aplikasi).
                onBack = { backDispatcher?.onBackPressed() }
            )
        }

        composable(Routes.REGISTER) {
            RegisterScreen(
                onRegisterSuccess = {
                    navController.navigate(Routes.MAIN) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                },
                onNavigateToLogin = { navController.popBackStack() },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.MAIN) {
            MainScreen(
                onOpenFeature = { name -> navController.navigate(Routes.todo(name)) },
                onLoggedOut = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(Routes.MAIN) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = Routes.TODO,
            arguments = listOf(navArgument(Routes.TODO_ARG_TITLE) { type = NavType.StringType })
        ) { entry ->
            TodoScreen(
                title = entry.arguments?.getString(Routes.TODO_ARG_TITLE).orEmpty(),
                onBack = { navController.popBackStack() }
            )
        }
    }
}

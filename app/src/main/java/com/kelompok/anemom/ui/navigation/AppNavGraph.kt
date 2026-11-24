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
import com.kelompok.anemom.ui.healthprofile.HealthProfileScreen
import com.kelompok.anemom.ui.onboarding.OnboardingScreen
import com.kelompok.anemom.ui.placeholder.TodoScreen
import com.kelompok.anemom.ui.splash.SplashScreen
import com.kelompok.anemom.ui.auth.ForgotPasswordScreen
import com.kelompok.anemom.ui.nutrisi.FoodDetailScreen
import com.kelompok.anemom.ui.nutrisi.MenuListScreen

/**
 * Alur utama:
 * Splash -> (Onboarding ->) Login <-> Register (langkah 1) -> Profil Kesehatan Awal (langkah 2)
 * -> Main (bottom nav) -> halaman fitur TODO.
 * Login akun yang langkah 2-nya belum diisi juga masuk ke Profil Kesehatan Awal dulu.
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
                onLoginSuccess = { profileCompleted ->
                    navController.navigate(if (profileCompleted) Routes.MAIN else Routes.HEALTH_PROFILE) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                },
                onNavigateToRegister = { navController.navigate(Routes.REGISTER) },
                onNavigateToForgotPassword = { navController.navigate(Routes.FORGOT_PASSWORD) }, // baru
                onBack = { backDispatcher?.onBackPressed() }
            )
        }

        composable(Routes.FORGOT_PASSWORD) {
            ForgotPasswordScreen(onBackToLogin = { navController.popBackStack() })
        }

        composable(Routes.REGISTER) {
            RegisterScreen(
                // Akun sudah dibuat; lanjut ke langkah 2 (belum ke dashboard).
                onRegisterSuccess = {
                    navController.navigate(Routes.HEALTH_PROFILE) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                },
                onNavigateToLogin = { navController.popBackStack() },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.HEALTH_PROFILE) {
            HealthProfileScreen(
                onSaved = {
                    navController.navigate(Routes.MAIN) {
                        popUpTo(Routes.HEALTH_PROFILE) { inclusive = true }
                    }
                },
                onLoggedOut = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(Routes.HEALTH_PROFILE) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.MAIN) {
            MainScreen(
                onOpenFeature = { name -> navController.navigate(Routes.todo(name)) },
                onOpenMenuList = { navController.navigate(Routes.MENU_LIST) },
                onOpenFoodDetail = { id -> navController.navigate(Routes.foodDetail(id)) },
                onLoggedOut = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(Routes.MAIN) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.MENU_LIST) {
            MenuListScreen(
                onBack = { navController.popBackStack() },
                onOpenDetail = { id -> navController.navigate(Routes.foodDetail(id)) }
            )
        }

        composable(
            route = Routes.FOOD_DETAIL,
            arguments = listOf(navArgument(Routes.FOOD_DETAIL_ARG_ID) { type = NavType.LongType })
        ) {
            FoodDetailScreen(onBack = { navController.popBackStack() })
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

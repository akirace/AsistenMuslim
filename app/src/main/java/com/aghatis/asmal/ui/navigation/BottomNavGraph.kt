package com.aghatis.asmal.ui.navigation


import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.aghatis.asmal.ui.assistant.AssistantScreen
import com.aghatis.asmal.ui.components.BottomNavItem
import com.aghatis.asmal.ui.home.HomeScreen
import com.aghatis.asmal.ui.menu.MenuTabScreen
import com.aghatis.asmal.ui.profile.ProfileScreen
import com.aghatis.asmal.ui.profile.SettingScreen

@Composable
fun BottomNavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = BottomNavItem.Home.route
    ) {
        composable(BottomNavItem.Home.route) {
            HomeScreen()
        }
        composable(BottomNavItem.Menu.route) {
            MenuTabScreen(
                onNavigateToQuran = {
                    navController.navigate("quran")
                }
            )
        }
        composable(BottomNavItem.Assistant.route) {
            // AssistantScreen()
        }
        composable(BottomNavItem.Profile.route) {
            ProfileScreen(onNavigateToSettings = { navController.navigate("settings") })
        }
        composable("settings") {
            SettingScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable("quran") {
            com.aghatis.asmal.ui.quran.QuranScreen(navController = navController)
        }
        composable(
            route = "quran_detail/{surahNo}",
            arguments = listOf(
                androidx.navigation.navArgument("surahNo") {
                    type = androidx.navigation.NavType.IntType
                }
            )
        ) { backStackEntry ->
            val surahNo = backStackEntry.arguments?.getInt("surahNo") ?: 1
            com.aghatis.asmal.ui.quran.QuranDetailScreen(
                navController = navController,
                surahNo = surahNo
            )
        }
    }
}

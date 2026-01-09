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
import com.aghatis.asmal.ui.quran.QuranViewModel
import com.aghatis.asmal.data.repository.QuranRepository
import com.aghatis.asmal.data.repository.QoriRepository
import com.aghatis.asmal.ui.quran.QuranPlayerScreen
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.remember

@Composable
fun BottomNavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = BottomNavItem.Home.route
    ) {
        // Shared Quran ViewModel setup
        // We put it inside the graph but lifted?
        // Actually, to share it, we need a scope.
        // A simple way for this app structure is to create it here (if BottomNavGraph is always active)
        // OR use the navigation graph entry scope.
        // Let's create it at NavHost level for simplicity as per current app architecture.
        
        composable(BottomNavItem.Home.route) {
            HomeScreen()
        }
        composable(BottomNavItem.Menu.route) {
            MenuTabScreen(
                onNavigateToQuran = {
                    navController.navigate("quran")
                },
                onNavigateToQibla = {
                    navController.navigate("qibla")
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
            val context = LocalContext.current
            // Shared instance setup logic can be here or lifted, but passing between "quran" and "quran_player" requires
            // them to be sharing a store or instance.
            // Since they are sibling destinations in the main graph, we can use the Activity scope or manually manage.
            // Let's use `viewModel(navController.getBackStackEntry("quran"))` if nested, but they are not.
            // Simpler approach: Create the ViewModel provider in the NavHost arguments or a parent wrapper.
            // BUT for now, let's create a specific ViewModel for the graph by using a helper or just creating it here and remembering it.
            // Note: `remember` keeps it across recompositions of NavHost, but NavHost itself might not stick.
            // However, MainActivity holds BottomNavGraph.
            
            // CORRECT APPROACH for shared VM in Navigation Component without nested graph:
            // Define the VM in the NavHost scope (MainActivity content) or use a hoisted state.
            // Since I cannot easily change MainActivity content structure without seeing it all, 
            // I will use a scoped ViewModel to the Activity (since audio player is somewhat global-ish).
            
            val quranViewModel: QuranViewModel = viewModel(
                // Use LocalContext (Activity) as owner? default viewModel() uses LocalViewModelStoreOwner which is the NavBackStackEntry.
                // If we want to share, we need same owner.
                // Let's use the Activity as the owner for the QuranViewModel to allow background play persistence across screens.
                viewModelStoreOwner = LocalContext.current as androidx.activity.ComponentActivity,
                factory = remember {
                    val db = androidx.room.Room.databaseBuilder(
                        context.applicationContext,
                        com.aghatis.asmal.data.local.AppDatabase::class.java, "asmal-db"
                    ).fallbackToDestructiveMigration().build()
                    val repo = QuranRepository(context)
                    val qoriRepo = QoriRepository(db.qoriDao())
                    QuranViewModel.Factory(repo, qoriRepo)
                }
            )
            
            com.aghatis.asmal.ui.quran.QuranScreen(navController = navController, viewModel = quranViewModel)
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
        
        composable(
            route = "quran_player/{surahNo}",
            arguments = listOf(
                androidx.navigation.navArgument("surahNo") {
                    type = androidx.navigation.NavType.IntType
                }
            )
        ) { backStackEntry ->
            val surahNo = backStackEntry.arguments?.getInt("surahNo") ?: 1
            val context = LocalContext.current
            
            // Retrieve the SAME ViewModel instance attached to Activity
            val quranViewModel: QuranViewModel = viewModel(
                viewModelStoreOwner = LocalContext.current as androidx.activity.ComponentActivity
            )
            
            QuranPlayerScreen(
                navController = navController,
                viewModel = quranViewModel,
                surahNo = surahNo
            )
        }

        composable("qibla") {
            com.aghatis.asmal.ui.qibla.QiblaScreen(onNavigateBack = { navController.popBackStack() })
        }
    }
}

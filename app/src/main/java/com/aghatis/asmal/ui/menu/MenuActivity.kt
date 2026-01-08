package com.aghatis.asmal.ui.menu

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import com.aghatis.asmal.ui.theme.AsistenAmalMuslimTheme
import com.aghatis.asmal.data.model.AppTheme
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.aghatis.asmal.data.repository.AuthRepository
import com.aghatis.asmal.data.repository.PrefsRepository
import com.aghatis.asmal.ui.login.LoginActivity

import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.aghatis.asmal.ui.components.CustomBottomNavigation
import com.aghatis.asmal.ui.navigation.BottomNavGraph

class MenuActivity : ComponentActivity() {

    private val authRepository by lazy { AuthRepository(applicationContext) }
    private val prefsRepository by lazy { PrefsRepository(applicationContext) }
    private val viewModel: MenuViewModel by viewModels {
        MenuViewModel.Factory(authRepository, prefsRepository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AsistenAmalMuslimTheme() {
                MenuScreen(viewModel = viewModel)
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun MenuScreen(viewModel: MenuViewModel) {
        val uiState by viewModel.uiState.collectAsState()
        val navController = rememberNavController()
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route
        val showBottomBar = currentRoute != "settings" && 
                           currentRoute != "quran" && 
                           currentRoute?.startsWith("quran_detail") != true &&
                           currentRoute?.startsWith("quran_player") != true

        LaunchedEffect(uiState) {
            if (uiState is MenuUiState.LoggedOut) {
                startActivity(Intent(this@MenuActivity, LoginActivity::class.java))
                finish()
            }
        }

        Scaffold(
            bottomBar = {
                if (showBottomBar) {
                    CustomBottomNavigation(navController = navController)
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                BottomNavGraph(navController = navController)
            }
        }
    }
}

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.aghatis.asmal.data.repository.AuthRepository
import com.aghatis.asmal.data.repository.PrefsRepository
import com.aghatis.asmal.ui.login.LoginActivity

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
            MenuScreen(viewModel = viewModel)
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun MenuScreen(viewModel: MenuViewModel) {
        val uiState by viewModel.uiState.collectAsState()
        var showMenu by remember { mutableStateOf(false) }
        val navController = rememberNavController()

        LaunchedEffect(uiState) {
            if (uiState is MenuUiState.LoggedOut) {
                startActivity(Intent(this@MenuActivity, LoginActivity::class.java))
                finish()
            }
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Menu") },
                    actions = {
                        IconButton(onClick = { showMenu = !showMenu }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More")
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Logout") },
                                onClick = {
                                    showMenu = false
                                    viewModel.logout()
                                }
                            )
                        }
                    }
                )
            },
            bottomBar = {
                CustomBottomNavigation(navController = navController)
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

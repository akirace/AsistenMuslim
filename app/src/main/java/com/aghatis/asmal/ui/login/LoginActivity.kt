package com.aghatis.asmal.ui.login

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.CustomCredential
import com.aghatis.asmal.R
import com.aghatis.asmal.data.repository.AuthRepository
import com.aghatis.asmal.data.repository.PrefsRepository
import com.aghatis.asmal.ui.menu.MenuActivity
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class LoginActivity : ComponentActivity() {

    private val authRepository by lazy { AuthRepository(applicationContext) }
    private val prefsRepository by lazy { PrefsRepository(applicationContext) }
    private val viewModel: LoginViewModel by viewModels {
        LoginViewModel.Factory(authRepository, prefsRepository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LoginScreen(viewModel = viewModel, onGoogleSignIn = {
                initiateGoogleSignIn()
            })
        }
    }

    private fun initiateGoogleSignIn() {
        val credentialManager = CredentialManager.create(this)
        
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(getString(R.string.default_web_client_id)) 
            .setAutoSelectEnabled(true)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()
        
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val result = credentialManager.getCredential(
                    request = request,
                    context = this@LoginActivity
                )
                
                when (val credential = result.credential) {
                    is CustomCredential -> {
                        if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                             val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                             viewModel.signInWithGoogle(googleIdTokenCredential.idToken)
                        } else {
                            Toast.makeText(this@LoginActivity, "Unexpected credential type", Toast.LENGTH_SHORT).show()
                        }
                    }
                    else -> {
                        Toast.makeText(this@LoginActivity, "Unexpected credential type", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: androidx.credentials.exceptions.GetCredentialException) {
                 if (e.message?.contains("android.os.CancellationSignal") == false) {
                    Toast.makeText(this@LoginActivity, "Authentication failed: ${e.message}", Toast.LENGTH_SHORT).show()
                 }
            } catch (e: Exception) {
                Toast.makeText(this@LoginActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    @Composable
    fun LoginScreen(viewModel: LoginViewModel, onGoogleSignIn: () -> Unit) {
        val uiState by viewModel.uiState.collectAsState()

        LaunchedEffect(uiState) {
            if (uiState is LoginUiState.Success) {
                startActivity(Intent(this@LoginActivity, MenuActivity::class.java))
                finish()
            }
        }

        Scaffold { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                when (uiState) {
                    is LoginUiState.Loading -> CircularProgressIndicator()
                    is LoginUiState.Error -> {
                        // Show error and button to retry
                         val error = (uiState as LoginUiState.Error).message
                         Toast.makeText(this@LoginActivity, error, Toast.LENGTH_SHORT).show()
                         Button(onClick = onGoogleSignIn) {
                            Text("Sign in with Google")
                        }
                    }
                    else -> {
                        Button(onClick = onGoogleSignIn) {
                            Text("Sign in with Google")
                        }
                    }
                }
            }
        }
    }
}

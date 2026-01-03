package com.aghatis.asmal.ui.login

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.aghatis.asmal.R
import com.aghatis.asmal.data.repository.AuthRepository
import com.aghatis.asmal.data.repository.PrefsRepository
import com.aghatis.asmal.ui.menu.MenuActivity
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class LoginActivity : ComponentActivity() {

    private val authRepository by lazy { AuthRepository(applicationContext) }
    private val prefsRepository by lazy { PrefsRepository(applicationContext) }
    private val viewModel: LoginViewModel by viewModels {
        LoginViewModel.Factory(authRepository, prefsRepository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
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
            } catch (e: GetCredentialException) {
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
        var startAnimation by remember { mutableStateOf(false) }

        LaunchedEffect(Unit) {
            startAnimation = true
        }

        LaunchedEffect(uiState) {
            if (uiState is LoginUiState.Success) {
                // Add a small delay for user to see success state if needed, or jump straight
                startActivity(Intent(this@LoginActivity, MenuActivity::class.java))
                finish()
            }
        }

        // Islamic Theme Colors
        val darkGreen = Color(0xFF0F3D3E)
        val teal = Color(0xFF1FAB89)
        val lightTeal = Color(0xFFD2F5E3)
        val gold = Color(0xFFFFD700)

        val gradientBrush = Brush.verticalGradient(
            colors = listOf(darkGreen, teal),
            startY = 0f,
            endY = Float.POSITIVE_INFINITY
        )

        Scaffold { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(gradientBrush)
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                
                // Decorative Background Patterns (Optional - simplified as subtle circles/gradients)
                // You could add Image(painter = painterResource(id = R.drawable.islamic_pattern)...) here

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(32.dp)
                ) {
                    
                    AnimatedVisibility(
                        visible = startAnimation,
                        enter = fadeIn(animationSpec = tween(1000)) + slideInVertically(initialOffsetY = { -40 }, animationSpec = tween(1000))
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_logo_google),
                            contentDescription = "App Logo",
                            modifier = Modifier
                                .size(120.dp)
                                .padding(bottom = 24.dp),
                            contentScale = ContentScale.Fit
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    AnimatedVisibility(
                        visible = startAnimation,
                        enter = fadeIn(animationSpec = tween(1000, delayMillis = 300))
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Assalamu'alaikum",
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                ),
                                textAlign = TextAlign.Center
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "Selamat Datang di AsistenMuslim",
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    color = lightTeal
                                ),
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(64.dp))

                    AnimatedVisibility(
                        visible = startAnimation,
                        enter = fadeIn(animationSpec = tween(1000, delayMillis = 600)) + slideInVertically(initialOffsetY = { 40 }, animationSpec = tween(1000, delayMillis = 600))
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            when (uiState) {
                                is LoginUiState.Loading -> {
                                    CircularProgressIndicator(
                                        color = gold,
                                        modifier = Modifier.size(48.dp)
                                    )
                                }
                                is LoginUiState.Error -> {
                                    val error = (uiState as LoginUiState.Error).message
                                    Text(
                                        text = error,
                                        color = Color.Red, // Or a softer error color appropriate for theme
                                        modifier = Modifier.padding(bottom = 8.dp),
                                        textAlign = TextAlign.Center
                                    )
                                    GoogleSignInButton(onClick = onGoogleSignIn)
                                }
                                else -> {
                                    GoogleSignInButton(onClick = onGoogleSignIn)
                                }
                            }
                        }
                    }
                }
                
                // Footer / Copyright
                 Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 24.dp),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Text(
                        text = "Spirituality in your hand",
                        style = MaterialTheme.typography.labelMedium.copy(color = Color.White.copy(alpha = 0.5f))
                    )
                }
            }
        }
    }

    @Composable
    fun GoogleSignInButton(onClick: () -> Unit) {
        Button(
            onClick = onClick,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.White,
                contentColor = Color.Black
            ),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            elevation = ButtonDefaults.buttonElevation(
                defaultElevation = 6.dp,
                pressedElevation = 2.dp
            )
        ) {
            // Since we don't have a Google Icon strictly guaranteed, we verify if we can use a standard icon or text
            // Ideally: Icon(painter = painterResource(id = R.drawable.ic_google), contentDescription = null, tint = Color.Unspecified)
            // For now, we use a placeholder text style
            Text(
                text = "Sign in with Google",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            )
        }
    }
}

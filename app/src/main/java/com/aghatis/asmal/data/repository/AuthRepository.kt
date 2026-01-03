package com.aghatis.asmal.data.repository

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import com.aghatis.asmal.R
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.tasks.await

class AuthRepository(private val context: Context) {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val credentialManager: CredentialManager = CredentialManager.create(context)

    suspend fun firebaseAuthWithGoogle(idToken: String): Result<FirebaseUser> {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val authResult = auth.signInWithCredential(credential).await()
            val user = authResult.user
            if (user != null) {
                Result.success(user)
            } else {
                Result.failure(Exception("User is null"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    fun signOut() {
        auth.signOut()
    }

    fun getCurrentUser(): FirebaseUser? {
        return auth.currentUser
    }
}

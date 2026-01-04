package com.aghatis.asmal.data.repository

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.aghatis.asmal.data.model.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import com.aghatis.asmal.data.model.AppTheme


private val Context.dataStore by preferencesDataStore(name = "user_prefs")

class PrefsRepository(private val context: Context) {

    companion object {
        private val KEY_IS_LOGGED_IN = booleanPreferencesKey("is_logged_in")
        private val KEY_USER_ID = stringPreferencesKey("user_id")
        private val KEY_DISPLAY_NAME = stringPreferencesKey("display_name")
        private val KEY_EMAIL = stringPreferencesKey("email")
        private val KEY_PHOTO_URL = stringPreferencesKey("photo_url")
        private val KEY_THEME = stringPreferencesKey("app_theme")
    }

    val isLoggedIn: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_IS_LOGGED_IN] ?: false
    }

    val userData: Flow<User?> = context.dataStore.data.map { preferences ->
        if (preferences[KEY_IS_LOGGED_IN] == true) {
            User(
                id = preferences[KEY_USER_ID] ?: "",
                displayName = preferences[KEY_DISPLAY_NAME] ?: "",
                email = preferences[KEY_EMAIL] ?: "",
                photoUrl = preferences[KEY_PHOTO_URL]
            )
        } else {
            null
        }
    }

    suspend fun saveUserSession(user: User) {
        context.dataStore.edit { preferences ->
            preferences[KEY_IS_LOGGED_IN] = true
            preferences[KEY_USER_ID] = user.id
            preferences[KEY_DISPLAY_NAME] = user.displayName
            preferences[KEY_EMAIL] = user.email
            user.photoUrl?.let { preferences[KEY_PHOTO_URL] = it }
        }
    }

    suspend fun clearSession() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }
    val selectedTheme: Flow<AppTheme> = context.dataStore.data.map { preferences ->
        val themeName = preferences[KEY_THEME] ?: AppTheme.MATERIAL_YOU.name
        try {
            AppTheme.valueOf(themeName)
        } catch (e: Exception) {
            AppTheme.MATERIAL_YOU
        }
    }

    suspend fun saveTheme(theme: AppTheme) {
        context.dataStore.edit { preferences ->
            preferences[KEY_THEME] = theme.name
        }
    }
}

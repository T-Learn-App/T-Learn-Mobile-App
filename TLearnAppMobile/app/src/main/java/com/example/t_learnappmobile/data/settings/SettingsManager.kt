package com.example.t_learnappmobile.data.settings

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.edit
import com.example.t_learnappmobile.data.repository.ServiceLocator
import kotlinx.coroutines.flow.firstOrNull

class SettingsManager(private val context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_THEME = "theme_mode"
        private const val KEY_CATEGORY_ID = "current_category_id"
        const val THEME_LIGHT = AppCompatDelegate.MODE_NIGHT_NO
        const val THEME_DARK = AppCompatDelegate.MODE_NIGHT_YES
        const val THEME_SYSTEM = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
    }

    fun getTheme(): Int = prefs.getInt(KEY_THEME, THEME_SYSTEM)

    fun setTheme(mode: Int) {
        prefs.edit { putInt(KEY_THEME, mode) }
        AppCompatDelegate.setDefaultNightMode(mode)
    }

    fun saveUserProfile(firstName: String, lastName: String) {
        prefs.edit {
            putString("user_first_name", firstName)
            putString("user_last_name", lastName)
        }
    }

    fun getFirstName(): String = prefs.getString("user_first_name", "") ?: ""
    fun getLastName(): String = prefs.getString("user_last_name", "") ?: ""

    fun getCurrentCategoryId(): Long {
        return prefs.getLong(KEY_CATEGORY_ID, 1L)
    }

    fun setCurrentCategoryId(categoryId: Long) {
        prefs.edit { putLong(KEY_CATEGORY_ID, categoryId) }
    }

    suspend fun updateUserProfile(firstName: String, lastName: String): Boolean {
        saveUserProfile(firstName, lastName)

        return try {
            val accessToken = ServiceLocator.tokenManager.getAccessToken().firstOrNull()
            if (accessToken != null) {
                val response = ServiceLocator.userApiService.updateProfile(
                    "Bearer $accessToken",
                    com.example.t_learnappmobile.data.user.UserProfileUpdateRequest(firstName, lastName)
                )
                response.isSuccessful
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    suspend fun clearAllData() {
        prefs.edit { clear() }
        AppCompatDelegate.setDefaultNightMode(THEME_SYSTEM)
    }
}
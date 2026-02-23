package com.example.t_learnappmobile.data.settings

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.edit
import com.example.t_learnappmobile.data.dictionary.DictionaryManager
import com.example.t_learnappmobile.data.repository.ServiceLocator
import kotlinx.coroutines.flow.firstOrNull
class SettingsManager(private val context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
    private val dictionaryManager = DictionaryManager(context)

    companion object {
        private const val KEY_THEME = "theme_mode"
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

    suspend fun updateUserProfile(firstName: String, lastName: String) {
        saveUserProfile(firstName, lastName)
        val currentUser = ServiceLocator.tokenManager.getUserData().firstOrNull()
        currentUser?.let {
            ServiceLocator.tokenManager.saveUserData(
                it.copy(firstName = firstName, lastName = lastName)
            )
        }
    }

    fun getDictionaryManager(): DictionaryManager = dictionaryManager

    suspend fun clearDictionaryData(userId: Int) {
        dictionaryManager.clearCurrentDictionaryStats(userId)
    }

    suspend fun clearAllData() {
        prefs.edit { clear() }
        dictionaryManager.clearAllDictionaries()
        AppCompatDelegate.setDefaultNightMode(THEME_SYSTEM)
    }
}

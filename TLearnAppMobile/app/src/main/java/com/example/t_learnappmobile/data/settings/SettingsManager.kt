package com.example.t_learnappmobile.data.settings

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.edit
import com.example.t_learnappmobile.data.repository.ServiceLocator

class SettingsManager(private val context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_THEME = "theme_mode"
        private const val KEY_CATEGORY_ID = "current_category_id"
        private const val KEY_CATEGORY_NAME = "current_category_name"
        const val THEME_LIGHT = AppCompatDelegate.MODE_NIGHT_NO
        const val THEME_DARK = AppCompatDelegate.MODE_NIGHT_YES
        const val THEME_SYSTEM = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
    }

    fun getTheme(): Int = prefs.getInt(KEY_THEME, THEME_SYSTEM)

    fun setTheme(mode: Int) {
        prefs.edit { putInt(KEY_THEME, mode) }
    }


    fun getCurrentCategoryId(): String? {
        return prefs.getString(KEY_CATEGORY_ID, null)
    }

    fun setCurrentCategoryId(categoryId: String) {
        prefs.edit { putString(KEY_CATEGORY_ID, categoryId) }
    }

    fun getCurrentDictionaryName(): String? =
        prefs.getString(KEY_CATEGORY_NAME, null)

    fun setCurrentDictionaryName(name: String) {
        prefs.edit { putString(KEY_CATEGORY_NAME, name) }
    }


    fun hasSelectedDictionary(): Boolean {
        val value = prefs.getString(KEY_CATEGORY_ID, null)
        return !value.isNullOrBlank()
    }

    suspend fun updateUserProfile(firstName: String, lastName: String): Boolean {
        return ServiceLocator.userRepository.updateProfile(firstName, lastName)
    }


     fun clearAllData() {
        prefs.edit {
            remove(KEY_THEME)
            remove(KEY_CATEGORY_ID)
            remove(KEY_CATEGORY_NAME)
        }
        AppCompatDelegate.setDefaultNightMode(THEME_SYSTEM)
    }
}
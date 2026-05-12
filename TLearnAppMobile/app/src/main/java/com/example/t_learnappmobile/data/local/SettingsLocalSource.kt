// data/local/SettingsLocalSource.kt
package com.example.t_learnappmobile.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

class SettingsLocalSource(private val context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_THEME = "theme_mode"
        private const val KEY_CATEGORY_ID = "current_category_id"
        private const val KEY_CATEGORY_NAME = "current_category_name"

        const val THEME_LIGHT = 1
        const val THEME_DARK = 2
        const val THEME_SYSTEM = 0
    }

    fun getTheme(): Int = prefs.getInt(KEY_THEME, THEME_SYSTEM)

    fun setTheme(mode: Int) {
        prefs.edit { putInt(KEY_THEME, mode) }
    }

    fun getCurrentDictionaryId(): String? = prefs.getString(KEY_CATEGORY_ID, null)

    fun setCurrentDictionaryId(categoryId: String) {
        prefs.edit { putString(KEY_CATEGORY_ID, categoryId) }
    }

    fun getCurrentDictionaryName(): String? = prefs.getString(KEY_CATEGORY_NAME, null)

    fun setCurrentDictionaryName(name: String) {
        prefs.edit { putString(KEY_CATEGORY_NAME, name) }
    }

    fun clearAllData() {
        prefs.edit {
            remove(KEY_THEME)
            remove(KEY_CATEGORY_ID)
            remove(KEY_CATEGORY_NAME)
        }
    }
}
// data/local/SettingsLocalSource.kt
package com.example.t_learnappmobile.data.local

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
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


    fun clearAllData() {
        prefs.edit {
            remove(KEY_THEME)
            remove(KEY_CATEGORY_ID)
            remove(KEY_CATEGORY_NAME)
        }
    }
    // data/local/SettingsLocalSource.kt
// Добавьте логирование в методы:

    fun getCurrentDictionaryId(): String? {
        val value = prefs.getString(KEY_CATEGORY_ID, null)
        Log.d("SettingsLocal", "getCurrentDictionaryId: '$value'")
        return value
    }

    fun setCurrentDictionaryId(categoryId: String) {
        Log.d("SettingsLocal", "setCurrentDictionaryId: '$categoryId'")
        prefs.edit { putString(KEY_CATEGORY_ID, categoryId) }
    }

    fun getCurrentDictionaryName(): String? {
        val value = prefs.getString(KEY_CATEGORY_NAME, null)
        Log.d("SettingsLocal", "getCurrentDictionaryName: '$value'")
        return value
    }

    fun setCurrentDictionaryName(name: String) {
        Log.d("SettingsLocal", "setCurrentDictionaryName: '$name'")
        prefs.edit { putString(KEY_CATEGORY_NAME, name) }
    }
}
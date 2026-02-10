package com.example.t_learnappmobile.data.settings

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.edit
import com.example.t_learnappmobile.data.dictionary.DictionaryManager

class SettingsManager(private val context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
    private val dictionaryManager = DictionaryManager(context)
    companion object {
        private const val KEY_THEME = "theme_mode"
        const val THEME_LIGHT = AppCompatDelegate.MODE_NIGHT_NO
        const val THEME_DARK = AppCompatDelegate.MODE_NIGHT_YES
        const val THEME_SYSTEM = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
    }

    fun getTheme(): Int {
        return prefs.getInt(KEY_THEME, THEME_SYSTEM)
    }
    fun setTheme(mode: Int){
        prefs.edit { putInt(KEY_THEME, mode) }
        AppCompatDelegate.setDefaultNightMode(mode)
    }

    fun getDictionaryManager(): DictionaryManager {
        return dictionaryManager
    }

    fun clearDictionaryData(userId: Int) {
        dictionaryManager.clearCurrentDictionaryStats(userId)
    }

    fun clearAllData() {
        prefs.edit { clear() }
        dictionaryManager.clearAllDictionaries()
        AppCompatDelegate.setDefaultNightMode(THEME_SYSTEM)
    }
}
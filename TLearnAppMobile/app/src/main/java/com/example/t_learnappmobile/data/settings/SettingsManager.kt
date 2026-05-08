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

    /**
     * Возвращает имя пользователя из Firebase (актуальное для текущего аккаунта)
     */
    suspend fun getFirstName(): String {
        val profile = ServiceLocator.userRepository.getCurrentUserProfile()
        return profile?.firstName ?: ""
    }

    /**
     * Возвращает фамилию пользователя из Firebase (актуальную для текущего аккаунта)
     */
    suspend fun getLastName(): String {
        val profile = ServiceLocator.userRepository.getCurrentUserProfile()
        return profile?.lastName ?: ""
    }
    // Файл: data/settings/SettingsManager.kt
// Добавьте этот метод в существующий класс SettingsManager

    /**
     * ИСПРАВЛЕНО: Получает имя пользователя напрямую из Firebase
     * вместо кэшированных данных
     */
    suspend fun getUserFirstName(): String {
        val profile = ServiceLocator.userRepository.getCurrentUserProfile()
        return profile?.firstName ?: ""
    }

    /**
     * ИСПРАВЛЕНО: Получает фамилию пользователя напрямую из Firebase
     */
    suspend fun getUserLastName(): String {
        val profile = ServiceLocator.userRepository.getCurrentUserProfile()
        return profile?.lastName ?: ""
    }

    /**
     * НОВЫЙ МЕТОД: Сбрасывает настройки текущего пользователя
     */
    fun resetUserSettings() {
        prefs.edit {
            remove(KEY_CATEGORY_ID)
            remove(KEY_CATEGORY_NAME)
        }
    }

    /**
     * Обновляет имя и фамилию в Firebase
     */
    suspend fun updateUserProfile(firstName: String, lastName: String): Boolean {
        return ServiceLocator.userRepository.updateProfile(firstName, lastName)
    }

    fun getCurrentCategoryId(): String =
        prefs.getString(KEY_CATEGORY_ID, "finance") ?: "finance"

    fun setCurrentCategoryId(categoryId: String) {
        prefs.edit { putString(KEY_CATEGORY_ID, categoryId) }
    }

    fun getCurrentDictionaryName(): String? =
        prefs.getString(KEY_CATEGORY_NAME, null)

    fun setCurrentDictionaryName(name: String) {
        prefs.edit { putString(KEY_CATEGORY_NAME, name) }
    }

    suspend fun clearAllData() {
        // Очищаем только тему и выбор словаря, но не имя/фамилию (они в Firebase)
        prefs.edit {
            remove(KEY_THEME)
            remove(KEY_CATEGORY_ID)
            remove(KEY_CATEGORY_NAME)
        }
        AppCompatDelegate.setDefaultNightMode(THEME_SYSTEM)
    }
}
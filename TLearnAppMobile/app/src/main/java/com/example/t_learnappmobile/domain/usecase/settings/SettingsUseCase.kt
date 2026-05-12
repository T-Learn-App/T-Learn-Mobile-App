// domain/usecase/settings/SettingsUseCase.kt
package com.example.t_learnappmobile.domain.usecase.settings

import com.example.t_learnappmobile.data.local.SettingsLocalSource

class SettingsUseCase(
    private val settingsLocalSource: SettingsLocalSource
) {
    fun getTheme(): Int = settingsLocalSource.getTheme()

    fun isDarkTheme(): Boolean = settingsLocalSource.getTheme() == SettingsLocalSource.THEME_DARK

    fun setTheme(isDark: Boolean) {
        settingsLocalSource.setTheme(
            if (isDark) SettingsLocalSource.THEME_DARK
            else SettingsLocalSource.THEME_LIGHT
        )
    }

    fun getCurrentDictionaryId(): String? = settingsLocalSource.getCurrentDictionaryId()

    fun getCurrentDictionaryName(): String? = settingsLocalSource.getCurrentDictionaryName()

    fun setCurrentDictionary(dictionaryId: String, name: String) {
        settingsLocalSource.setCurrentDictionaryId(dictionaryId)
        settingsLocalSource.setCurrentDictionaryName(name)
    }

    fun clearAllSettings() = settingsLocalSource.clearAllData()
}
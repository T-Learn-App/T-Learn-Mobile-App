package com.example.t_learnappmobile.data.dictionary

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

class DictionaryManager(private val context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("dictionary_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_CURRENT_DICTIONARY = "current_dictionary_id"
        private const val KEY_DICTIONARY_STATS = "dictionary_stats_"
    }

    fun getDictionaries(): List<Dictionary> {
        return listOf(
            Dictionary(
                id = 1,
                vocabularyId = 1,
                name = "Conversional",
                description = "Разговорные слова",
                wordsCount = 25
            ),
            Dictionary(
                id = 2,
                vocabularyId = 2,
                name = "Technologies",
                description = "Технологии",
                wordsCount = 35
            ),
            Dictionary(
                id = 3,
                vocabularyId = 3,
                name = "Slang",
                description = "Слэнг",
                wordsCount = 20
            )
        )
    }

    fun getCurrentDictionary(): Dictionary {
        val currentId = prefs.getInt(KEY_CURRENT_DICTIONARY, 1)
        return getDictionaries().find { it.id == currentId }
            ?: getDictionaries().first()
    }

    fun setCurrentDictionary(dictionaryId: Int) {
        prefs.edit {
            putInt(KEY_CURRENT_DICTIONARY, dictionaryId)
        }
    }

    fun getCurrentVocabularyId(): Int {
        return getCurrentDictionary().vocabularyId
    }
    fun clearCurrentDictionaryStats() {
        val currentDict = getCurrentDictionary()
        prefs.edit {
            remove("${KEY_DICTIONARY_STATS}${currentDict.id}")
        }
    }
    fun clearAllDictionaries() {
        prefs.edit { clear() }
    }
}


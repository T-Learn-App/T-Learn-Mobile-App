package com.example.t_learnappmobile.data.dictionary

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.example.t_learnappmobile.data.statistics.DailyStats
import java.text.SimpleDateFormat
import java.util.*

class DictionaryManager(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("dictionary_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_DICTIONARY_STATS = "dictionary_stats_"
        private const val KEY_STATS_PREFIX = "stats_"
    }

    private fun keyCurrentDictionary(userId: Int?) = "current_dictionary_id_user$userId"

    fun saveDailyStats(userId: Int, stats: DailyStats) {
        val currentDict = getCurrentDictionary(userId)
        val key = "${KEY_STATS_PREFIX}${userId}_${currentDict.id}_${stats.date}"
        prefs.edit {
            putInt("${key}_new", stats.newWords)
            putInt("${key}_inProgress", stats.inProgressWords)
            putInt("${key}_learned", stats.learnedWords)
        }
    }

    fun getDailyStats(userId: Int, date: String): DailyStats {
        val currentDict = getCurrentDictionary(userId)
        val key = "${KEY_STATS_PREFIX}${userId}_${currentDict.id}_${date}"
        return DailyStats(
            date = date,
            newWords = prefs.getInt("${key}_new", 0),
            inProgressWords = prefs.getInt("${key}_inProgress", 0),
            learnedWords = prefs.getInt("${key}_learned", 0)
        )
    }

    fun getLastWeekStats(userId: Int): List<DailyStats> {
        val calendar = Calendar.getInstance()
        val stats = mutableListOf<DailyStats>()
        repeat(7) {
            val dateStr = formatDate(calendar.time)
            stats.add(getDailyStats(userId, dateStr))
            calendar.add(Calendar.DAY_OF_YEAR, -1)
        }
        return stats.reversed()
    }

    fun getWeekLabel(userId: Int): String {
        val week = getLastWeekStats(userId)
        if (week.isEmpty()) return ""
        val first = week.first().date
        val last = week.last().date
        val inFmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val outFmt = SimpleDateFormat("dd.MM", Locale.getDefault())
        return try {
            val firstStr = outFmt.format(inFmt.parse(first)!!)
            val lastStr = outFmt.format(inFmt.parse(last)!!)
            "$firstStr - $lastStr"
        } catch (e: Exception) {
            ""
        }
    }

    private fun formatDate(date: Date): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(date)
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

    fun getCurrentDictionary(userId: Int): Dictionary {
        val currentId = prefs.getInt(keyCurrentDictionary(userId), 1)
        return getDictionaries().find { it.id == currentId } ?: getDictionaries().first()
    }

    fun setCurrentDictionary(userId: Int, dictionaryId: Int) {
        prefs.edit { putInt(keyCurrentDictionary(userId), dictionaryId) }
    }

    fun getCurrentVocabularyId(userId: Int): Int = getCurrentDictionary(userId).vocabularyId

    fun clearCurrentDictionaryStats(userId: Int) {
        val currentDict = getCurrentDictionary(userId)
        prefs.edit { remove("${KEY_DICTIONARY_STATS}${currentDict.id}") }
    }

    fun clearAllDictionaries() {
        prefs.edit { clear() }
    }
}
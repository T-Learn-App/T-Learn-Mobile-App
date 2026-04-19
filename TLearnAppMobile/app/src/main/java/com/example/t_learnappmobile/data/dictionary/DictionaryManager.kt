package com.example.t_learnappmobile.data.dictionary

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.example.t_learnappmobile.data.repository.ServiceLocator
import com.example.t_learnappmobile.data.statistics.DailyStats
import java.text.SimpleDateFormat
import java.util.*
import com.example.t_learnappmobile.data.statistics.DailyStatsDao
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class DictionaryManager(private val context: Context) {
    fun formatTodayDate(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences("dictionary_prefs", Context.MODE_PRIVATE)
    private val statsDao: DailyStatsDao by lazy { ServiceLocator.dailyStatsDao }


    private val _currentVocabularyId = MutableStateFlow<Int?>(null)
    val currentVocabularyIdFlow: StateFlow<Int?> = _currentVocabularyId

    fun initVocabulary(userId: Int) {
        val vocabId = getCurrentVocabularyId(userId)
        _currentVocabularyId.value = vocabId
    }

    fun setCurrentDictionary(userId: Int, dictionaryId: Int) {
        val currentId = prefs.getInt(keyCurrentDictionary(userId), -1)
        prefs.edit { putInt(keyCurrentDictionary(userId), dictionaryId) }
        val vocabId = getCurrentVocabularyId(userId)
        _currentVocabularyId.value = vocabId
    }

    private fun keyCurrentDictionary(userId: Int?) = "current_dictionary_id_user$userId"

    suspend fun saveDailyStats(userId: Int, stats: DailyStats) {
        val currentDict = getCurrentDictionary(userId)
        val entity = com.example.t_learnappmobile.data.statistics.DailyStatsEntity(
            userId = userId,
            dictionaryId = currentDict.id,
            date = stats.date,
            newWords = stats.newWords,
            inProgressWords = stats.inProgressWords,
            learnedWords = stats.learnedWords
        )
        statsDao.insertOrUpdate(entity)
    }

    suspend fun getDailyStats(userId: Int, date: String): DailyStats {
        val currentDict = getCurrentDictionary(userId)
        val entity = statsDao.getByDate(userId, currentDict.id, date)
        return if (entity != null) {
            DailyStats(date = date, newWords = entity.newWords, inProgressWords = entity.inProgressWords, learnedWords = entity.learnedWords)
        } else {
            DailyStats(date = date)
        }
    }




    suspend fun getLastWeekStats(userId: Int): List<DailyStats> {
        val currentDict = getCurrentDictionary(userId)
        val cal = Calendar.getInstance()
        val today = formatDate(cal.time)
        cal.add(Calendar.DAY_OF_YEAR, -6)
        val sixDaysAgo = formatDate(cal.time)

        val entities = statsDao.getStatsForPeriod(userId, currentDict.id, sixDaysAgo, today)
        
        val entityMap = entities.associateBy { it.date }

        val dates = mutableListOf<String>()
        val dateCal = Calendar.getInstance()
        dateCal.add(Calendar.DAY_OF_YEAR, -6)
        repeat(7) {
            dates.add(formatDate(dateCal.time))
            dateCal.add(Calendar.DAY_OF_YEAR, 1)
        }

        return dates.map { date ->
            val e = entityMap[date]
            DailyStats(
                date = date,
                newWords = e?.newWords ?: 0,
                inProgressWords = e?.inProgressWords ?: 0,
                learnedWords = e?.learnedWords ?: 0
            )
        }
    }


    suspend fun clearCurrentDictionaryStats(userId: Int) {
        val currentDict = getCurrentDictionary(userId)
        statsDao.deleteForDictionary(userId, currentDict.id)
    }

    suspend fun clearAllDictionaries() {
        prefs.edit { clear() }
        statsDao.deleteAll()
    }

    private fun formatDate(date: Date): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(date)
    }


    fun getDictionaries(): List<Dictionary> {
        return listOf(
            Dictionary(
                id = 1,
                vocabularyId = 1,
                name = "Conversational",
                description = "Разговорные слова",
                wordsCount = 22
            ),
            Dictionary(
                id = 2,
                vocabularyId = 2,
                name = "Technologies",
                description = "Технологии",
                wordsCount = 22
            ),
            Dictionary(
                id = 3,
                vocabularyId = 3,
                name = "Slang",
                description = "Слэнг",
                wordsCount = 22
            )
        )
    }

    fun getCurrentDictionary(userId: Int): Dictionary {
        val currentId = prefs.getInt(keyCurrentDictionary(userId), 1)
        return getDictionaries().find { it.id == currentId } ?: getDictionaries().first()
    }

    fun getCurrentVocabularyId(userId: Int): Int = getCurrentDictionary(userId).vocabularyId
}

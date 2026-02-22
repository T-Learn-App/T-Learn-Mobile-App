package com.example.t_learnappmobile.data.dictionary

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.example.t_learnappmobile.data.repository.ServiceLocator
import com.example.t_learnappmobile.data.statistics.DailyStats
import java.text.SimpleDateFormat
import java.util.*
import com.example.t_learnappmobile.data.statistics.DailyStatsDao
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class DictionaryManager(private val context: Context) {
    // Добавьте в DictionaryManager.kt:
    fun formatTodayDate(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences("dictionary_prefs", Context.MODE_PRIVATE)
    private val statsDao: DailyStatsDao by lazy { ServiceLocator.dailyStatsDao }

    companion object {
        private const val KEY_DICTIONARY_STATS = "dictionary_stats_"
        private const val KEY_STATS_PREFIX = "stats_"
    }

    private val _currentVocabularyId = MutableStateFlow<Int?>(null)
    val currentVocabularyIdFlow: StateFlow<Int?> = _currentVocabularyId

    fun setCurrentDictionary(userId: Int, dictionaryId: Int) {
        prefs.edit { putInt(keyCurrentDictionary(userId), dictionaryId) }
        _currentVocabularyId.value = getCurrentVocabularyId(userId)
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
            DailyStats(
                date = entity.date,
                newWords = entity.newWords,
                inProgressWords = entity.inProgressWords,
                learnedWords = entity.learnedWords
            )
        } else {
            DailyStats(date = date)
        }
    }

    // ✅ МОК: Создает тестовые данные при первом запуске
    suspend fun generateMockStats(userId: Int) {
        val currentDict = getCurrentDictionary(userId)
        val dates = mutableListOf<String>()
        val dateCal = Calendar.getInstance()
        dateCal.add(Calendar.DAY_OF_YEAR, -6)
        repeat(7) {
            dates.add(formatDate(dateCal.time))
            dateCal.add(Calendar.DAY_OF_YEAR, 1)
        }

        // ✅ РЕАЛИСТИЧНЫЕ мок-данные за неделю
        val mockData = listOf(
            Triple(dates[0], 3, 2),  // 22.02: 3 новых, 2 в процессе
            Triple(dates[1], 5, 1),  // 23.02: 5 новых, 1 в процессе
            Triple(dates[2], 2, 4),  // 24.02: 2 новых, 4 в процессе
            Triple(dates[3], 4, 3),  // 25.02: 4 новых, 3 в процессе
            Triple(dates[4], 1, 5),  // 26.02: 1 новый, 5 в процессе
            Triple(dates[5], 6, 0),  // 27.02: 6 новых, 0 в процессе
            Triple(dates[6], 0, 7)   // 28.02: 0 новых, 7 в процессе
        )

        mockData.forEach { (date, newWords, inProgress) ->
            val stats = DailyStats(
                date = date,
                newWords = newWords,
                inProgressWords = inProgress,
                learnedWords = (1..newWords).random() // случайное кол-во выученных
            )
            saveDailyStats(userId, stats)
            delay(50) // имитация задержки
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

    suspend fun getWeekLabel(userId: Int): String {
        val week = getLastWeekStats(userId)
        if (week.isEmpty()) return ""
        val first = week.first().date
        val last = week.last().date
        val inFmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val outFmt = SimpleDateFormat("dd.MM", Locale.getDefault())
        return try {
            "${outFmt.format(inFmt.parse(first)!!)} - ${outFmt.format(inFmt.parse(last)!!)}"
        } catch (e: Exception) {
            ""
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

    fun getDictionaryName(dictionaryId: Long): String {
        return getDictionaries().find { it.id.toLong() == dictionaryId }?.name ?: "Неизвестный словарь"
    }

    // ✅ УЛУЧШЕННАЯ заглушка с мок-данными
    suspend fun getTotalLearnedWords(userId: Int): Int {
        val currentDict = getCurrentDictionary(userId)
        // ✅ Имитируем выученные слова (20% от общего количества)
        return (currentDict.wordsCount * 0.2).toInt().coerceAtLeast(2)
    }

    fun getDictionaries(): List<Dictionary> {
        return listOf(
            Dictionary(
                id = 1,
                vocabularyId = 1,
                name = "Conversational",
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

    fun getCurrentVocabularyId(userId: Int): Int = getCurrentDictionary(userId).vocabularyId
}

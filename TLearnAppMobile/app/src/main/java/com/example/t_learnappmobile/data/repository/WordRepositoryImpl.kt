package com.example.t_learnappmobile.data.repository

import StatQueueDto
import WordResponse
import android.util.Log
import com.example.t_learnappmobile.data.repository.ServiceLocator

import com.example.t_learnappmobile.domain.repository.WordRepository
import com.example.t_learnappmobile.model.CardType
import com.example.t_learnappmobile.model.PartOfSpeech
import com.example.t_learnappmobile.model.TranslationDirection
import com.example.t_learnappmobile.model.Word
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import retrofit2.HttpException

class WordRepositoryImpl(
    private val api: WordApi,
    private val storage: WordsStorage
) : WordRepository {

    override fun nextWord() {
        storage.nextWord()
    }

    override fun getCurrentCardFlow(): Flow<Word?> {
        return storage.currentCardFlow
    }

    override fun getCurrentCard(): Word? {
        return storage.getCurrentWord()
    }

    override fun getNewWords(): List<Word> {
        return storage.getNewWords()
    }

    override fun getLearnedWords(): List<Word> {
        return storage.getLearnedWords()
    }

    override fun getRotationWords(): List<Word> {
        return storage.getRotationWords()
    }

    override fun addWord(word: Word) {
        storage.addWord(word)
    }
    override suspend fun fetchWords(categoryId: Long): List<Word> {
        Log.d("WordRepository", "🔄 Загрузка слов для categoryId=$categoryId")

        val token = getAccessTokenSync()
        Log.d("WordRepository", "🔑 Токен: ${if (token != null) "есть" else "нет"}")

        if (token == null) {
            throw Exception("Необходима авторизация")
        }

        try {
            val response = api.getWordsByCategory("Bearer $token", categoryId)
            Log.d("WordRepository", "📡 Response: ${response.code()} ${response.body()}")

            if (!response.isSuccessful) {
                throw Exception("HTTP ${response.code()}")
            }

            val body = response.body()
            val words = if (body != null) {
                body.words.map { mapBackendWord(it) }
            } else {
                listOf(createTestWord()) // временный фоллбэк
            }

            Log.d("WordRepository", "✅ Загружено ${words.size} слов")
            storage.updateWords(words)
            return words

        } catch (e: Exception) {
            Log.e("WordRepository", "❌ Ошибка", e)
            val testWords = listOf(createTestWord())
            storage.updateWords(testWords)
            return testWords
        }
    }


    private fun mapBackendWord(w: WordResponse): Word {
        return Word(
            id = w.id,
            vocabularyId = w.category.toInt(),
            englishWord = w.word,
            transcription = w.transcription,
            partOfSpeech = mapPartOfSpeech(w.partOfSpeech),  // ✅ Теперь НЕ null!
            russianTranslation = w.translation ?: "перевод",
            category = "Category ${w.category}",
            cardType = CardType.NEW,
            repetitionStage = 0,
            isLearned = false,
            translationDirection = TranslationDirection.ENGLISH_TO_RUSSIAN
        )
    }

    private fun mapPartOfSpeech(s: String): PartOfSpeech = when (s.lowercase()) {
        "noun" -> PartOfSpeech.NOUN
        "adjective" -> PartOfSpeech.ADJECTIVE
        "verb" -> PartOfSpeech.VERB
        "pronoun" -> PartOfSpeech.PRONOUN
        "adverb" -> PartOfSpeech.ADVERB
        else -> PartOfSpeech.INTERJECTION
    }

    private fun createTestWord(): Word = Word(
        id = 1,
        vocabularyId = 1,
        englishWord = "hello",
        transcription = "/hɛˈloʊ/",
        partOfSpeech = PartOfSpeech.NOUN,
        russianTranslation = "привет",
        category = "Category 1",
        cardType = CardType.NEW,
        repetitionStage = 0,
        isLearned = false,
        translationDirection = TranslationDirection.ENGLISH_TO_RUSSIAN
    )



    override suspend fun completeWord(wordId: Long): Boolean {
        val success = try {
            val token = getAccessTokenSync() ?: return false
            val response = api.completeWord("Bearer $token", StatQueueDto(wordId))
            response.isSuccessful
        } catch (e: Exception) {
            Log.e("WordRepository", "completeWord error", e)
            false
        }
        if (success) {
            updateStatsAfterWordCompletion(wordId)
        }
        return success
    }

    private suspend fun updateStatsAfterWordCompletion(wordId: Long) {
        val currentWord = storage.getCurrentWord()
        if (currentWord == null || currentWord.id != wordId) return

        val userId = ServiceLocator.tokenManager.getUserId()?.toInt() ?: 0
        val dictionaryManager = ServiceLocator.dictionaryManager
        val today = dictionaryManager.formatTodayDate()

        val currentStats = dictionaryManager.getDailyStats(userId, today)

        val updatedStats = when {
            currentWord.cardType == CardType.NEW -> currentStats.copy(
                learnedWords = currentStats.learnedWords + if (isKnowAction(wordId)) 1 else 0,
                newWords = currentStats.newWords + if (!isKnowAction(wordId)) 1 else 0
            )
            currentWord.cardType == CardType.ROTATION -> currentStats.copy(
                learnedWords = currentStats.learnedWords + if (isKnowAction(wordId)) 1 else 0,
                inProgressWords = currentStats.inProgressWords + if (!isKnowAction(wordId)) 1 else 0
            )
            else -> currentStats
        }

        dictionaryManager.saveDailyStats(userId, updatedStats)
    }

    private fun isKnowAction(wordId: Long): Boolean {
        return wordId % 2 == 0L
    }

    private suspend fun getAccessTokenSync(): String? =
        runBlocking { ServiceLocator.tokenManager.getAccessToken().firstOrNull() }
}

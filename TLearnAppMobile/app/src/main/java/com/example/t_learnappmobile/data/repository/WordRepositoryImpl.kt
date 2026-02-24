package com.example.t_learnappmobile.data.repository

import com.example.t_learnappmobile.data.repository.ServiceLocator
import com.example.t_learnappmobile.domain.model.StatQueueDto
import com.example.t_learnappmobile.domain.model.WordResponse
import com.example.t_learnappmobile.domain.repository.WordRepository
import com.example.t_learnappmobile.model.CardType
import com.example.t_learnappmobile.model.PartOfSpeech
import com.example.t_learnappmobile.model.TranslationDirection
import com.example.t_learnappmobile.model.Word
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking

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
        var fetchedWords: List<Word>

        try {
            val token = getAccessTokenSync()
            if (token != null) {
                val response = api.getWordsByCategory("Bearer $token", categoryId)
                if (response.isSuccessful && response.body() != null) {
                    fetchedWords = response.body()!!.words.map { mapBackendWord(it) }
                } else {
                    fetchedWords = getMockBatch(0, categoryId.toInt(), 10)
                }
            } else {
                fetchedWords = getMockBatch(0, categoryId.toInt(), 10)
            }
        } catch (e: Exception) {
            fetchedWords = getMockBatch(0, categoryId.toInt(), 10)
        }

        storage.updateWords(fetchedWords)
        return fetchedWords
    }

    private fun mapBackendWord(w: WordResponse): Word {
        val wordIndex = (w.id % 5).toInt()
        val cardType = when (wordIndex) {
            0, 1 -> CardType.NEW
            2, 3, 4 -> CardType.ROTATION
            else -> CardType.NEW
        }
        val repetitionStage = when (wordIndex) {
            2 -> 1
            3 -> 2
            4 -> 3
            else -> 0
        }

        return Word(
            id = w.id,
            vocabularyId = w.category.toInt(),
            englishWord = w.word,
            transcription = w.transcription,
            partOfSpeech = mapPartOfSpeech(w.partOfSpeech),
            russianTranslation = w.translation,
            category = "Category ${w.category}",
            cardType = cardType,
            repetitionStage = repetitionStage,
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

    override suspend fun completeWord(wordId: Long): Boolean {
        val success = try {
            val token = getAccessTokenSync() ?: return false
            val response = api.completeWord("Bearer $token", StatQueueDto(wordId))
            response.isSuccessful
        } catch (e: Exception) {
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

    private fun getMockBatch(userId: Int, vocabularyId: Int, batchSize: Int): List<Word> {
        val currentDict = ServiceLocator.dictionaryManager.getCurrentDictionary(userId)
        val wordsMap = mapOf(
            1 to listOf("Hello", "Goodbye", "Please", "Thank you", "Sorry"),
            2 to listOf("Computer", "Software", "Internet", "Data", "Algorithm"),
            3 to listOf("Cool", "Awesome", "Bro", "Dude", "Sick")
        )
        val translationsMap = mapOf(
            1 to listOf("Привет", "До свидания", "Пожалуйста", "Спасибо", "Извините"),
            2 to listOf("Компьютер", "ПО", "Интернет", "Данные", "Алгоритм"),
            3 to listOf("Круто", "Отлично", "Братан", "Чувак", "Круто")
        )

        val words = wordsMap[vocabularyId] ?: wordsMap[1]!!
        val translations = translationsMap[vocabularyId] ?: translationsMap[1]!!

        return List(batchSize) { index ->
            val wordIndex = index % 5
            val cardType = when (wordIndex) {
                0, 1 -> CardType.NEW
                else -> CardType.ROTATION
            }
            val repetitionStage = when (wordIndex) {
                2 -> 1
                3 -> 2
                4 -> 3
                else -> 0
            }

            Word(
                id = index + 1.toLong(),
                vocabularyId = vocabularyId,
                englishWord = words.getOrElse(index % words.size) { "Word${index + 1}" },
                transcription = "[həˈləʊ]",
                partOfSpeech = PartOfSpeech.INTERJECTION,
                russianTranslation = translations.getOrElse(index % translations.size) { "Слово${index + 1}" },
                category = currentDict.name,
                cardType = cardType,
                repetitionStage = repetitionStage,
                isLearned = false,
                translationDirection = TranslationDirection.ENGLISH_TO_RUSSIAN
            )
        }
    }
}

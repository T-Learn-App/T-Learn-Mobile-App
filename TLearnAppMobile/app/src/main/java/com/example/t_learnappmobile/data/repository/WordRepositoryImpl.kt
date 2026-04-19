package com.example.t_learnappmobile.data.repository

import StatQueueDto
import WordResponse
import android.util.Log
import com.example.t_learnappmobile.domain.model.CardAction
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

    val apiService: WordApi get() = api

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

        val token = getAccessTokenSync()

        if (token == null) {
            throw Exception("Необходима авторизация")
        }

        try {

            val response = api.getWordsByCategory("Bearer $token", categoryId)


            if (!response.isSuccessful) {
                val errorBody = response.errorBody()?.string()

                throw Exception("HTTP ${response.code()}: $errorBody")
            }

            val body = response.body()

            
            val words = body?.words?.map { 
                val mapped = mapBackendWord(it)
                mapped
            } ?: emptyList()



            
            storage.updateWords(words)
            return words

        } catch (e: Exception) {
            throw e
        }
    }


    private fun mapBackendWord(w: WordResponse): Word {
        return Word(
            id = w.id,
            vocabularyId = w.category.toInt(),
            englishWord = w.word,
            transcription = w.transcription,
            partOfSpeech = mapPartOfSpeech(w.partOfSpeech),
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




    override suspend fun completeWord(wordId: Long, action: CardAction): Boolean {
        val success = try {
            val token = getAccessTokenSync() ?: return false
            val request: CompleteStatsRequest = CompleteStatsRequest(
                wordId = wordId,
                isCorrect = action == CardAction.KNOW
            )
            val response = api.completeWord("Bearer $token", request)
            response.isSuccessful
        } catch (e: Exception) {

            false
        }

        updateStatsAfterWordCompletion(wordId, action)
        return success
    }

    private suspend fun updateStatsAfterWordCompletion(wordId: Long, action: CardAction) {
        val currentWord = storage.getCurrentWord()
        if (currentWord == null) {
            return
        }

        val userId = ServiceLocator.tokenManager.getUserId()?.toInt() ?: 0
        val dictionaryManager = ServiceLocator.dictionaryManager
        val today = dictionaryManager.formatTodayDate()

        val currentStats = dictionaryManager.getDailyStats(userId, today)
        
        val isKnow = action == CardAction.KNOW
        val isNewWord = currentWord.cardType == CardType.NEW
        
        val updatedStats = if (isKnow) {

            currentStats.copy(
                learnedWords = currentStats.learnedWords + 1,
                newWords = if (isNewWord) currentStats.newWords + 1 else currentStats.newWords
            )
        } else {
            currentStats.copy(
                inProgressWords = currentStats.inProgressWords + 1,
                newWords = if (isNewWord) currentStats.newWords + 1 else currentStats.newWords
            )
        }

        dictionaryManager.saveDailyStats(userId, updatedStats)

    }

    private suspend fun getAccessTokenSync(): String? =
        runBlocking { ServiceLocator.tokenManager.getAccessToken().firstOrNull() }
}

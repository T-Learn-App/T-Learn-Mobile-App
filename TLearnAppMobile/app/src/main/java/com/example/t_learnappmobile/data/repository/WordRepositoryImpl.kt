package com.example.t_learnappmobile.data.repository

import android.util.Log
import com.example.t_learnappmobile.domain.model.WordResponse
import com.example.t_learnappmobile.domain.repository.WordRepository
import com.example.t_learnappmobile.model.CardType
import com.example.t_learnappmobile.model.PartOfSpeech
import com.example.t_learnappmobile.model.TranslationDirection
import com.example.t_learnappmobile.model.Word
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking

class WordRepositoryImpl(
    private val api: WordApi,
    private val storage: WordsStorage
) : WordRepository {

    override fun nextWord() {
        storage.nextWord()
    }

    override fun getCurrentCardFlow() = storage.currentCardFlow

    override fun getCurrentCard() = storage.getCurrentWord()

    override fun getNewWords() = storage.getNewWords()

    override fun getLearnedWords() = storage.getLearnedWords()

    override fun getRotationWords() = storage.getRotationWords()

    override fun addWord(word: Word) = storage.addWord(word)

    override suspend fun fetchWords(categoryId: Long): List<Word> {
        val token = getAccessTokenSync()
            ?: throw Exception("Необходима авторизация")

        val response = api.getWordsByCategory("Bearer $token", categoryId)
        if (!response.isSuccessful) {
            throw Exception("HTTP ${response.code()}: ${response.errorBody()?.string()}")
        }

        val body = response.body()
        val words = body?.words?.map { mapBackendWord(it) } ?: emptyList()

        storage.updateWords(words)
        return words
    }

    override suspend fun fetchAllWords(): List<Word> {
        val token = getAccessTokenSync()
            ?: throw Exception("Необходима авторизация")

        val response = api.getAllWords("Bearer $token")
        if (!response.isSuccessful) {
            throw Exception("HTTP ${response.code()}: ${response.errorBody()?.string()}")
        }

        return response.body()?.words?.map { mapBackendWord(it) } ?: emptyList()
    }

    override suspend fun completeWord(wordId: Long): Boolean {
        return try {
            val token = getAccessTokenSync()
            if (token == null) {
                Log.e("WordRepository", "❌ Token is null")
                return false
            }

            Log.d("WordRepository", "📡 Calling completeWord API for wordId: $wordId")

            val request = CompleteWordRequest(wordId = wordId)
            val response = api.completeWord("Bearer $token", request)

            Log.d("WordRepository", "Response code: ${response.code()}")

            if (response.isSuccessful) {
                Log.d("WordRepository", "✅ Word completed successfully: $wordId")
            } else {
                val errorBody = response.errorBody()?.string()
                Log.e("WordRepository", "❌ Failed to complete word: ${response.code()} - $errorBody")
            }

            response.isSuccessful
        } catch (e: Exception) {
            Log.e("WordRepository", "❌ Exception in completeWord", e)
            false
        }
    }

    private fun mapBackendWord(w: WordResponse): Word {
        return Word(
            id = w.id,
            vocabularyId = w.category.toInt(),
            englishWord = w.word,
            transcription = w.transcription,
            partOfSpeech = mapPartOfSpeech(w.partOfSpeech),
            russianTranslation = w.translation,
            category = "Category ${w.category}",
            cardType = CardType.NEW,
            repetitionStage = 0,
            isLearned = false,
            translationDirection = TranslationDirection.ENGLISH_TO_RUSSIAN
        )
    }
    private fun mapPartOfSpeech(s: String?): PartOfSpeech = when (s?.lowercase()) {
        "noun" -> PartOfSpeech.NOUN
        "adjective" -> PartOfSpeech.ADJECTIVE
        "verb" -> PartOfSpeech.VERB
        "pronoun" -> PartOfSpeech.PRONOUN
        "adverb" -> PartOfSpeech.ADVERB
        null, "" -> PartOfSpeech.INTERJECTION
        else -> PartOfSpeech.INTERJECTION
    }

    private suspend fun getAccessTokenSync(): String? =
        runBlocking { ServiceLocator.tokenManager.getAccessToken().firstOrNull() }
}
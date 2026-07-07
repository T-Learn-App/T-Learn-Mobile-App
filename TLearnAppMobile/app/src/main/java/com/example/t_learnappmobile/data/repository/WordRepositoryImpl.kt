// data/repository/WordRepositoryImpl.kt (НОВАЯ ВЕРСИЯ)
package com.example.t_learnappmobile.data.repository

import android.util.Log
import com.example.t_learnappmobile.data.database.TLearnDatabase
import com.example.t_learnappmobile.data.database.WordEntity
import com.example.t_learnappmobile.domain.model.WordResponse
import com.example.t_learnappmobile.domain.repository.WordRepository
import com.example.t_learnappmobile.model.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.runBlocking

class WordRepositoryImpl(
    private val api: WordApi,
    private val storage: WordsStorage,
    private val database: TLearnDatabase
) : WordRepository {

    private val wordDao = database.wordDao()

    override fun nextWord() = storage.nextWord()
    override fun getCurrentCardFlow() = storage.currentCardFlow
    override fun getCurrentCard() = storage.getCurrentWord()
    override fun getNewWords() = storage.getNewWords()
    override fun getLearnedWords() = storage.getLearnedWords()
    override fun getRotationWords() = storage.getRotationWords()
    override fun addWord(word: Word) = storage.addWord(word)

    override suspend fun fetchWords(categoryId: Long): List<Word> {
        val cachedWords = wordDao.getWordsByCategory(categoryId.toString())
        if (cachedWords.isNotEmpty()) {
            Log.d("WordRepo", "✅ Загружено ${cachedWords.size} слов из Room (кэш)")
            val domainWords = cachedWords.map { it.toWord() }
            storage.updateWords(domainWords)
            return domainWords
        }


        Log.d("WordRepo", "⚠️ Кэш пуст, запрашиваю слова с сервера...")
        return fetchWordsFromNetworkAndCache(categoryId)
    }

    override suspend fun fetchAllWords(): List<Word> {
        val cachedWords = wordDao.getAllWords()
        if (cachedWords.isNotEmpty()) {
            return cachedWords.map { it.toWord() }
        }
        return fetchAllWordsFromNetworkAndCache()
    }

    private suspend fun fetchWordsFromNetworkAndCache(categoryId: Long): List<Word> {
        val token = getAccessTokenSync() ?: throw Exception("Необходима авторизация")
        val response = api.getWordsByCategory("Bearer $token", categoryId)
        if (!response.isSuccessful) throw Exception("HTTP ${response.code()}")

        val words = response.body()?.words?.map { mapBackendWord(it) } ?: emptyList()
        val entities = words.map { WordEntity.fromWord(it) }
        wordDao.insertAll(entities)
        Log.d("WordRepo", "💾 ${entities.size} слов сохранено в Room")

        storage.updateWords(words)
        return words
    }

    private suspend fun fetchAllWordsFromNetworkAndCache(): List<Word> {
        val token = getAccessTokenSync() ?: throw Exception("Необходима авторизация")
        val response = api.getAllWords("Bearer $token")
        if (!response.isSuccessful) throw Exception("HTTP ${response.code()}")

        val words = response.body()?.words?.map { mapBackendWord(it) } ?: emptyList()
        val entities = words.map { WordEntity.fromWord(it) }
        wordDao.insertAll(entities)
        return words
    }

    override suspend fun completeWord(wordId: Long): Boolean {
        return try {
            val token = getAccessTokenSync()
            if (token == null) return false
            val response = api.completeWord("Bearer $token", CompleteWordRequest(wordId))
            response.isSuccessful
        } catch (e: Exception) {
            Log.e("WordRepo", "Ошибка сервера, но слово помечено в кэше?")
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

    private fun mapPartOfSpeech(s: String?) = when (s?.lowercase()) {
        "noun" -> PartOfSpeech.NOUN
        "adjective" -> PartOfSpeech.ADJECTIVE
        "verb" -> PartOfSpeech.VERB
        else -> PartOfSpeech.INTERJECTION
    }

    private suspend fun getAccessTokenSync(): String? =
        runBlocking { ServiceLocator.tokenManager.getAccessToken().firstOrNull() }
}
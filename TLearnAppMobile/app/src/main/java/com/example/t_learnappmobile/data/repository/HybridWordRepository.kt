package com.example.t_learnappmobile.data.repository

import android.content.Context
import android.util.Log
import com.example.t_learnappmobile.data.firebase.FirebaseWordRepository
import com.example.t_learnappmobile.data.local.AppDatabase
import com.example.t_learnappmobile.data.local.entities.*
import com.example.t_learnappmobile.domain.repository.LoadWordsResult
import com.example.t_learnappmobile.domain.repository.WordRepository
import com.example.t_learnappmobile.model.*
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class HybridWordRepository(
    private val context: Context,
    private val firebaseRepo: FirebaseWordRepository
) : WordRepository {

    private val database = AppDatabase.getInstance(context)
    private val wordDao = database.wordDao()
    private val TAG = "HybridWordRepo"

    override suspend fun loadWords(dictionaryId: String): LoadWordsResult {
        val userId = ServiceLocator.firebaseAuthManager.getUserId()
        if (userId == null) {
            Log.e(TAG, "User not authenticated")
            return LoadWordsResult.Error("Пользователь не авторизован")
        }

        return withContext(Dispatchers.IO) {
            // 1. Пытаемся загрузить из Room
            val localWords = wordDao.getWordsByDictionary(dictionaryId)
            val localUserWords = wordDao.getUserWords(userId, dictionaryId)

            if (localWords.isNotEmpty() && localUserWords.isNotEmpty()) {
                Log.d(TAG, "✅ Loading from local DB: ${localWords.size} words")

                // Конвертируем в модель Word
                val wordsWithProgress = convertToWords(localWords, localUserWords)

                if (wordsWithProgress.isNotEmpty()) {
                    return@withContext LoadWordsResult.HasWords
                }
            }

            // 2. Если нет локально — грузим из Firebase
            Log.d(TAG, "🔄 Loading from Firebase...")
            val result = firebaseRepo.loadWords(dictionaryId)

            if (result is LoadWordsResult.HasWords) {
                // Сохраняем в Room для следующих разов
                saveToLocalCache(dictionaryId, userId)
            }

            return@withContext result
        }
    }

    private suspend fun saveToLocalCache(dictionaryId: String, userId: String) {
        try {
            val firestore = Firebase.firestore

            // Сохраняем слова
            val wordsSnapshot = firestore.collection("words")
                .whereEqualTo("dictionaryId", dictionaryId)
                .get()
                .await()

            val words = wordsSnapshot.documents.mapNotNull { doc ->
                val data = doc.data ?: return@mapNotNull null
                WordEntity(
                    id = doc.id,
                    dictionaryId = data["dictionaryId"] as? String ?: "",
                    englishWord = data["englishWord"] as? String ?: "",
                    translation = data["translation"] as? String ?: "",
                    transcription = data["transcription"] as? String ?: "",
                    partOfSpeech = data["partOfSpeech"] as? String ?: ""
                )
            }
            wordDao.insertWords(words)

            // Сохраняем прогресс
            val userWordsSnapshot = firestore.collection("user_words")
                .whereEqualTo("userId", userId)
                .whereEqualTo("dictionaryId", dictionaryId)
                .get()
                .await()

            val userWords = userWordsSnapshot.documents.mapNotNull { doc ->
                val data = doc.data ?: return@mapNotNull null
                UserWordEntity(
                    userId = userId,
                    wordId = data["wordId"] as? String ?: "",
                    dictionaryId = dictionaryId,
                    stage = (data["stage"] as? Long)?.toInt() ?: 0,
                    nextReviewDate = data["nextReviewDate"] as? Long ?: 0,
                    failCount = (data["failCount"] as? Long)?.toInt() ?: 0,
                    lastReviewDate = data["lastReviewDate"] as? Long,
                    totalViews = (data["totalViews"] as? Long)?.toInt() ?: 0,
                    correctCount = (data["correctCount"] as? Long)?.toInt() ?: 0,
                    incorrectCount = (data["incorrectCount"] as? Long)?.toInt() ?: 0,
                    isSynced = true
                )
            }
            userWords.forEach { wordDao.insertUserWord(it) }

            Log.d(TAG, "💾 Cached ${words.size} words and ${userWords.size} progress entries")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving to cache", e)
        }
    }

    private suspend fun convertToWords(
        words: List<WordEntity>,
        userWords: List<UserWordEntity>
    ): List<Word> {
        val userWordsMap = userWords.associateBy { it.wordId }
        val now = System.currentTimeMillis()

        return words.mapNotNull { wordEntity ->
            val userWord = userWordsMap[wordEntity.id]
            if (userWord == null) return@mapNotNull null

            Word(
                id = wordEntity.id,
                dictionaryId = wordEntity.dictionaryId,
                englishWord = wordEntity.englishWord,
                translation = wordEntity.translation,
                transcription = wordEntity.transcription,
                partOfSpeech = parsePartOfSpeech(wordEntity.partOfSpeech),
                stage = userWord.stage,
                nextReviewDate = userWord.nextReviewDate,
                isNew = userWord.stage == 0 && userWord.failCount == 0,
                userWordDocId = "${userWord.userId}_${wordEntity.id}"
            )
        }.filter { it.stage < 8 && it.nextReviewDate <= now }
    }

    // Делегируем остальные методы Firebase репозиторию
    override suspend fun getDictionaries(): List<Dictionary> = firebaseRepo.getDictionaries()

    override fun answerWord(wordId: String, known: Boolean) = firebaseRepo.answerWord(wordId, known)

    override fun getCurrentWordFlow(): Flow<Word?> = firebaseRepo.getCurrentWordFlow()

    override fun markCurrentWordAsShown() = firebaseRepo.markCurrentWordAsShown()

    private fun parsePartOfSpeech(value: String?): PartOfSpeech {
        return when (value?.lowercase()) {
            "noun" -> PartOfSpeech.NOUN
            "verb" -> PartOfSpeech.VERB
            "adjective" -> PartOfSpeech.ADJECTIVE
            "adverb" -> PartOfSpeech.ADVERB
            "pronoun" -> PartOfSpeech.PRONOUN
            "preposition" -> PartOfSpeech.PREPOSITION
            "conjunction" -> PartOfSpeech.CONJUNCTION
            "interjection" -> PartOfSpeech.INTERJECTION
            else -> PartOfSpeech.UNKNOWN
        }
    }
}
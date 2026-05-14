// data/repository/WordRepositoryImpl.kt
package com.example.t_learnappmobile.data.repository

import android.util.Log
import com.example.t_learnappmobile.data.local.WordLocalSource
import com.example.t_learnappmobile.data.local.entities.UserWordEntity
import com.example.t_learnappmobile.data.local.entities.WordEntity
import com.example.t_learnappmobile.data.remote.FirebaseFirestoreSource
import com.example.t_learnappmobile.domain.model.*
import com.example.t_learnappmobile.domain.repository.LoadWordsResult
import com.example.t_learnappmobile.domain.repository.WordRepository

class WordRepositoryImpl(
    private val localSource: WordLocalSource,
    private val remoteSource: FirebaseFirestoreSource
) : WordRepository {

    private val TAG = "WordRepository"

    private val reviewIntervals = listOf(
        0L, 5 * 60 * 1000L, 10 * 60 * 1000L, 60 * 60 * 1000L,
        24 * 60 * 60 * 1000L, 7 * 24 * 60 * 60 * 1000L,
        30L * 24 * 60 * 60 * 1000, 90L * 24 * 60 * 60 * 1000
    )

    override suspend fun loadWords(userId: String, dictionaryId: String): LoadWordsResult {
        Log.d(TAG, "Loading words for userId=$userId, dictionaryId=$dictionaryId")

        return try {
            // Сначала пробуем локальные данные
            val localWords = localSource.getWords(dictionaryId)
            val localProgress = localSource.getUserProgress(userId, dictionaryId)

            if (localWords.isNotEmpty()) {
                val progress = if (localProgress.isEmpty()) {
                    createInitialProgress(userId, dictionaryId, localWords)
                    localSource.getUserProgress(userId, dictionaryId)
                } else {
                    localProgress
                }

                val words = buildWordList(localWords, progress)
                if (words.isNotEmpty()) {
                    LoadWordsResult.HasWords(words)
                } else {
                    LoadWordsResult.Empty
                }
            } else {
                // Загружаем из Firebase
                loadFromRemote(userId, dictionaryId)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading words", e)
            LoadWordsResult.Error(e.message ?: "Unknown error")
        }
    }

    override suspend fun getDictionaries(): List<Dictionary> {
        return try {
            val localDicts = localSource.getDictionaries()
            if (localDicts.isNotEmpty()) {
                return localDicts.map { Dictionary(it.id, it.name, it.order) }
            }

            val remoteDicts = remoteSource.getDictionaries()
            if (remoteDicts.isNotEmpty()) {
                localSource.insertDictionaries(remoteDicts)
            }

            remoteDicts.ifEmpty {
                listOf(
                    com.example.t_learnappmobile.data.local.entities.DictionaryEntity("finance", "Финансы", 1),
                    com.example.t_learnappmobile.data.local.entities.DictionaryEntity("conversational", "Разговорные слова", 2),
                    com.example.t_learnappmobile.data.local.entities.DictionaryEntity("technology", "Технологии", 3),
                    com.example.t_learnappmobile.data.local.entities.DictionaryEntity("slang", "Сленг", 4)
                )
            }.map { Dictionary(it.id, it.name, it.order) }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading dictionaries", e)
            listOf(
                Dictionary("finance", "Финансы", 1),
                Dictionary("conversational", "Разговорные слова", 2),
                Dictionary("technology", "Технологии", 3),
                Dictionary("slang", "Сленг", 4)
            )
        }
    }

    override suspend fun processAnswer(
        userId: String,
        wordId: String,
        dictionaryId: String,
        known: Boolean
    ): Word? {
        val existingProgress = localSource.getUserWord(userId, wordId) ?: return null
        val wordEntity = localSource.getWords(dictionaryId).find { it.id == wordId } ?: return null

        val now = System.currentTimeMillis()
        val (newStage, nextReviewDate, newFailCount) = calculateNextStage(
            existingProgress.stage, known, existingProgress.failCount, now
        )

        val updatedProgress = UserWordEntity(
            userId = userId,
            wordId = wordId,
            dictionaryId = dictionaryId,
            stage = newStage,
            nextReviewDate = nextReviewDate,
            failCount = newFailCount,
            lastReviewDate = now,
            totalViews = existingProgress.totalViews + 1,
            correctCount = existingProgress.correctCount + (if (known) 1 else 0),
            incorrectCount = existingProgress.incorrectCount + (if (known) 0 else 1),
            isSynced = false,
            updatedAt = now
        )

        localSource.saveUserProgress(updatedProgress)

        // Если слово выучено (stage >= 8), возвращаем null
        return if (newStage < 8) {
            mapToDomain(wordEntity, updatedProgress)
        } else {
            null
        }
    }

    override suspend fun getStats(userId: String, dictionaryId: String): WordStats {
        val progress = localSource.getUserProgress(userId, dictionaryId)
        return WordStats(
            newWords = progress.count { it.stage == 0 },
            inProgressWords = progress.count { it.stage in 1..7 },
            learnedWords = progress.count { it.stage >= 8 }
        )
    }

    override suspend fun resetDictionaryProgress(userId: String, dictionaryId: String) {
        val words = localSource.getWords(dictionaryId)
        val now = System.currentTimeMillis()

        words.forEach { word ->
            localSource.saveUserProgress(
                UserWordEntity(
                    userId = userId,
                    wordId = word.id,
                    dictionaryId = dictionaryId,
                    stage = 0,
                    nextReviewDate = now,
                    failCount = 0,
                    lastReviewDate = null,
                    totalViews = 0,
                    correctCount = 0,
                    incorrectCount = 0,
                    isSynced = false,
                    updatedAt = now
                )
            )
        }
    }

    override suspend fun resetAllProgress(userId: String) {
        val dictionaries = localSource.getDictionaries()
        dictionaries.forEach { dict ->
            resetDictionaryProgress(userId, dict.id)
        }
    }

    // data/repository/WordRepositoryImpl.kt
// Замените метод createInitialProgress:

    private suspend fun createInitialProgress(
        userId: String,
        dictionaryId: String,
        words: List<WordEntity>
    ) {
        val now = System.currentTimeMillis()
        words.forEach { word ->
            try {
                localSource.saveUserProgress(
                    UserWordEntity(
                        userId = userId,
                        wordId = word.id,
                        dictionaryId = dictionaryId,
                        stage = 0,
                        nextReviewDate = now,
                        failCount = 0,
                        lastReviewDate = null,
                        totalViews = 0,
                        correctCount = 0,
                        incorrectCount = 0,
                        isSynced = false,
                        updatedAt = now
                    )
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error saving progress for word ${word.id}", e)
            }
        }
    }

    private suspend fun loadFromRemote(userId: String, dictionaryId: String): LoadWordsResult {
        val remoteWords = remoteSource.getWords(dictionaryId)
        if (remoteWords.isEmpty()) return LoadWordsResult.Empty

        localSource.insertWords(remoteWords)
        createInitialProgress(userId, dictionaryId, remoteWords)

        val remoteProgress = remoteSource.getUserProgress(userId, dictionaryId)
        if (remoteProgress.isNotEmpty()) {
            remoteProgress.forEach { localSource.saveUserProgress(it) }
        }

        val finalProgress = localSource.getUserProgress(userId, dictionaryId)
        val words = buildWordList(remoteWords, finalProgress)

        return if (words.isNotEmpty()) {
            LoadWordsResult.HasWords(words)
        } else {
            LoadWordsResult.Empty
        }
    }

    private fun buildWordList(words: List<WordEntity>, progress: List<UserWordEntity>): List<Word> {
        val progressMap = progress.associateBy { it.wordId }
        val now = System.currentTimeMillis()

        return words.mapNotNull { wordEntity ->
            val userProgress = progressMap[wordEntity.id] ?: return@mapNotNull null

            // Возвращаем только слова, которые нужно показать
            if (userProgress.stage < 8 && (userProgress.stage == 0 || userProgress.nextReviewDate <= now)) {
                mapToDomain(wordEntity, userProgress)
            } else {
                null
            }
        }.sortedBy { word ->
            when {
                word.isNew -> 0
                word.nextReviewDate <= now -> 1
                else -> 2
            }
        }
    }

    private fun mapToDomain(wordEntity: WordEntity, progress: UserWordEntity): Word {
        return Word(
            id = wordEntity.id,
            dictionaryId = wordEntity.dictionaryId,
            englishWord = wordEntity.englishWord,
            translation = wordEntity.translation,
            transcription = wordEntity.transcription,
            partOfSpeech = parsePartOfSpeech(wordEntity.partOfSpeech),
            stage = progress.stage,
            nextReviewDate = progress.nextReviewDate,
            isNew = progress.stage == 0 && progress.failCount == 0,
            userWordDocId = "${progress.userId}_${wordEntity.id}",
            failCount = progress.failCount
        )
    }

    private fun calculateNextStage(
        currentStage: Int,
        known: Boolean,
        failCount: Int,
        now: Long
    ): Triple<Int, Long, Int> {
        val maxStage = reviewIntervals.size - 1

        return if (known) {
            when {
                currentStage == 0 -> Triple(8, Long.MAX_VALUE, 0)
                currentStage < maxStage -> {
                    val newStage = currentStage + 1
                    Triple(newStage, now + reviewIntervals[newStage], 0)
                }
                else -> Triple(8, Long.MAX_VALUE, 0)
            }
        } else {
            val newFailCount = failCount + 1
            when {
                currentStage == 0 -> Triple(1, now + reviewIntervals[1], newFailCount)
                currentStage >= 1 && newFailCount <= 2 -> {
                    val retryInterval = if (newFailCount == 1) reviewIntervals[1] else reviewIntervals[2]
                    Triple(currentStage, now + retryInterval, newFailCount)
                }
                else -> {
                    val newStage = maxOf(1, currentStage - 1)
                    Triple(newStage, now + reviewIntervals[newStage], 0)
                }
            }
        }
    }

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
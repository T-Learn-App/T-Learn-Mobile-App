package com.example.t_learnappmobile.data.repository

import android.util.Log
import com.example.t_learnappmobile.data.local.WordLocalSource
import com.example.t_learnappmobile.data.local.entities.UserWordEntity
import com.example.t_learnappmobile.data.local.entities.WordEntity
import com.example.t_learnappmobile.data.remote.FirebaseFirestoreSource
import com.example.t_learnappmobile.domain.model.*
import com.example.t_learnappmobile.domain.repository.LoadWordsResult
import com.example.t_learnappmobile.domain.repository.WordRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.withContext

class WordRepositoryImpl(
    private val localSource: WordLocalSource,
    private val remoteSource: FirebaseFirestoreSource
) : WordRepository {

    private val TAG = "WordRepository"
    private val LEARNED_STAGE = 8

    private val reviewIntervals = listOf(
        0L,
        5 * 60 * 1000L,
        10 * 60 * 1000L,
        60 * 60 * 1000L,
        24 * 60 * 60 * 1000L,
        7 * 24 * 60 * 60 * 1000L,
        30L * 24 * 60 * 60 * 1000,
        90L * 24 * 60 * 60 * 1000,
        Long.MAX_VALUE
    )

    override suspend fun loadWords(userId: String, dictionaryId: String): LoadWordsResult {
        Log.d(TAG, "Loading words for userId=$userId, dictionaryId=$dictionaryId")

        return withContext(Dispatchers.IO) {
            try {
                val localWords = localSource.getWords(dictionaryId)
                val localProgress = localSource.getUserProgress(userId, dictionaryId)

                if (localWords.isNotEmpty()) {
                    val progress = if (localProgress.isEmpty()) {
                        try {
                            withTimeoutOrNull(3000L) {
                                syncProgressFromFirebase(userId, dictionaryId, localWords)
                            } ?: localProgress
                        } catch (e: Exception) {
                            Log.w(TAG, "Could not sync from Firebase", e)
                            localProgress
                        }
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
                    loadFromRemoteWithTimeout(userId, dictionaryId)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading words", e)
                LoadWordsResult.Empty
            }
        }
    }

    private suspend fun loadFromRemoteWithTimeout(userId: String, dictionaryId: String): LoadWordsResult {
        return try {
            withTimeoutOrNull(5000L) {
                val remoteWords = remoteSource.getWords(dictionaryId)
                if (remoteWords.isNotEmpty()) {
                    localSource.insertWords(remoteWords)
                    createInitialProgress(userId, dictionaryId, remoteWords)
                    val finalProgress = localSource.getUserProgress(userId, dictionaryId)
                    val words = buildWordList(remoteWords, finalProgress)
                    if (words.isNotEmpty()) LoadWordsResult.HasWords(words) else LoadWordsResult.Empty
                } else {
                    LoadWordsResult.Empty
                }
            } ?: LoadWordsResult.Empty
        } catch (e: Exception) {
            Log.e(TAG, "Error loading from remote", e)
            LoadWordsResult.Empty
        }
    }

    private suspend fun syncProgressFromFirebase(
        userId: String,
        dictionaryId: String,
        localWords: List<WordEntity>
    ): List<UserWordEntity> {
        try {
            Log.d(TAG, "Syncing progress from Firebase for dictionary: $dictionaryId")
            val remoteProgress = remoteSource.getUserProgress(userId, dictionaryId)

            if (remoteProgress.isNotEmpty()) {
                Log.d(TAG, "Found ${remoteProgress.size} progress entries in Firebase")
                remoteProgress.forEach { remoteProgressItem ->
                    localSource.saveUserProgress(remoteProgressItem)
                }
                return remoteProgress
            } else {
                Log.d(TAG, "No progress in Firebase, creating initial progress")
                createInitialProgress(userId, dictionaryId, localWords)
                return localSource.getUserProgress(userId, dictionaryId)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing progress from Firebase", e)
            createInitialProgress(userId, dictionaryId, localWords)
            return localSource.getUserProgress(userId, dictionaryId)
        }
    }

    override suspend fun getDictionaries(): List<Dictionary> {
        return withContext(Dispatchers.IO) {
            try {
                val localDicts = localSource.getDictionaries()
                if (localDicts.isNotEmpty()) {
                    return@withContext localDicts.map { Dictionary(it.id, it.name, it.order) }
                }

                val remoteDicts = withTimeoutOrNull(5000L) {
                    remoteSource.getDictionaries()
                } ?: emptyList()

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
    }

    override suspend fun processAnswer(
        userId: String,
        wordId: String,
        dictionaryId: String,
        known: Boolean
    ): Word? {
        return withContext(Dispatchers.IO) {
            val existingProgress = localSource.getUserWord(userId, wordId) ?: return@withContext null
            val wordEntity = localSource.getWords(dictionaryId).find { it.id == wordId } ?: return@withContext null

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
            Log.d(TAG, "Progress saved locally for word: $wordId, stage: $newStage")

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    withTimeoutOrNull(3000L) {
                        remoteSource.saveUserProgress(updatedProgress)
                        localSource.markAsSynced(userId, wordId)
                        Log.d(TAG, "Progress synced to Firebase for word: $wordId")
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Could not sync to Firebase: ${e.message}")
                }
            }

            return@withContext if (newStage < LEARNED_STAGE) {
                mapToDomain(wordEntity, updatedProgress)
            } else {
                null
            }
        }
    }

    override suspend fun getStats(userId: String, dictionaryId: String): WordStats {
        return withContext(Dispatchers.IO) {
            val progress = localSource.getUserProgress(userId, dictionaryId)
            WordStats(
                newWords = progress.count { it.stage == 0 },
                inProgressWords = progress.count { it.stage in 1 until LEARNED_STAGE },
                learnedWords = progress.count { it.stage >= LEARNED_STAGE }
            )
        }
    }

    override suspend fun resetDictionaryProgress(userId: String, dictionaryId: String) {
        withContext(Dispatchers.IO) {
            val words = localSource.getWords(dictionaryId)
            val now = System.currentTimeMillis()

            words.forEach { word ->
                val resetProgress = UserWordEntity(
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
                localSource.saveUserProgress(resetProgress)
            }
            Log.d(TAG, "Reset dictionary progress locally for $dictionaryId")
        }
    }

    override suspend fun resetAllProgress(userId: String) {
        withContext(Dispatchers.IO) {
            val dictionaries = localSource.getDictionaries()
            dictionaries.forEach { dict ->
                resetDictionaryProgress(userId, dict.id)
            }
            Log.d(TAG, "Reset all progress locally for user $userId")
        }
    }

    override suspend fun resetDictionaryProgressAndSync(userId: String, dictionaryId: String) {
        withContext(Dispatchers.IO) {
            try {
                remoteSource.deleteUserProgress(userId, dictionaryId)

                val words = localSource.getWords(dictionaryId)
                val now = System.currentTimeMillis()

                localSource.getUserProgress(userId, dictionaryId).forEach { progress ->
                    val resetProgress = UserWordEntity(
                        userId = userId,
                        wordId = progress.wordId,
                        dictionaryId = dictionaryId,
                        stage = 0,
                        nextReviewDate = now,
                        failCount = 0,
                        lastReviewDate = null,
                        totalViews = 0,
                        correctCount = 0,
                        incorrectCount = 0,
                        isSynced = true,
                        updatedAt = now
                    )
                    localSource.saveUserProgress(resetProgress)
                }

                if (words.isEmpty()) {
                    val remoteWords = remoteSource.getWords(dictionaryId)
                    if (remoteWords.isNotEmpty()) {
                        localSource.insertWords(remoteWords)
                        remoteWords.forEach { word ->
                            val resetProgress = UserWordEntity(
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
                                isSynced = true,
                                updatedAt = now
                            )
                            localSource.saveUserProgress(resetProgress)
                        }
                    }
                }

                Log.d(TAG, "Reset dictionary progress and synced for $dictionaryId")
            } catch (e: Exception) {
                Log.e(TAG, "Error resetting dictionary progress", e)
                resetDictionaryProgress(userId, dictionaryId)
            }
        }
    }

    override suspend fun resetAllProgressAndSync(userId: String) {
        withContext(Dispatchers.IO) {
            try {
                remoteSource.deleteAllUserProgress(userId)

                val dictionaries = localSource.getDictionaries()
                val now = System.currentTimeMillis()

                dictionaries.forEach { dict ->
                    val words = localSource.getWords(dict.id)

                    localSource.getUserProgress(userId, dict.id).forEach { progress ->
                        val resetProgress = UserWordEntity(
                            userId = userId,
                            wordId = progress.wordId,
                            dictionaryId = dict.id,
                            stage = 0,
                            nextReviewDate = now,
                            failCount = 0,
                            lastReviewDate = null,
                            totalViews = 0,
                            correctCount = 0,
                            incorrectCount = 0,
                            isSynced = true,
                            updatedAt = now
                        )
                        localSource.saveUserProgress(resetProgress)
                    }

                    if (words.isEmpty()) {
                        val remoteWords = remoteSource.getWords(dict.id)
                        if (remoteWords.isNotEmpty()) {
                            localSource.insertWords(remoteWords)
                            remoteWords.forEach { word ->
                                val resetProgress = UserWordEntity(
                                    userId = userId,
                                    wordId = word.id,
                                    dictionaryId = dict.id,
                                    stage = 0,
                                    nextReviewDate = now,
                                    failCount = 0,
                                    lastReviewDate = null,
                                    totalViews = 0,
                                    correctCount = 0,
                                    incorrectCount = 0,
                                    isSynced = true,
                                    updatedAt = now
                                )
                                localSource.saveUserProgress(resetProgress)
                            }
                        }
                    }
                }
                Log.d(TAG, "Reset all progress and synced for user $userId")
            } catch (e: Exception) {
                Log.e(TAG, "Error resetting all progress", e)
                resetAllProgress(userId)
            }
        }
    }

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

    private fun buildWordList(words: List<WordEntity>, progress: List<UserWordEntity>): List<Word> {
        val progressMap = progress.associateBy { it.wordId }
        val now = System.currentTimeMillis()

        return words.mapNotNull { wordEntity ->
            val userProgress = progressMap[wordEntity.id] ?: return@mapNotNull null

            if (userProgress.stage < LEARNED_STAGE &&
                (userProgress.stage == 0 || userProgress.nextReviewDate <= now)) {
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

    private fun calculateNextStage(
        currentStage: Int,
        known: Boolean,
        failCount: Int,
        now: Long
    ): Triple<Int, Long, Int> {
        return if (known) {
            when {
                currentStage == 0 -> {
                    Log.d(TAG, "Пользователь знает новое слово! Сразу выучено.")
                    Triple(LEARNED_STAGE, Long.MAX_VALUE, 0)
                }
                currentStage in 1 until LEARNED_STAGE -> {
                    val newStage = currentStage + 1
                    val nextDate = if (newStage < LEARNED_STAGE) {
                        now + reviewIntervals[newStage]
                    } else {
                        Long.MAX_VALUE
                    }
                    Triple(newStage, nextDate, 0)
                }
                else -> Triple(LEARNED_STAGE, Long.MAX_VALUE, 0)
            }
        } else {
            val newFailCount = failCount + 1
            when {
                currentStage == 0 -> {
                    Log.d(TAG, "Новое слово, пользователь не знает -> этап 1")
                    Triple(1, now + reviewIntervals[1], newFailCount)
                }
                currentStage in 1 until LEARNED_STAGE && newFailCount <= 2 -> {
                    val retryInterval = when (newFailCount) {
                        1 -> reviewIntervals[1]
                        else -> reviewIntervals[2]
                    }
                    Triple(currentStage, now + retryInterval, newFailCount)
                }
                currentStage in 1 until LEARNED_STAGE -> {
                    val newStage = maxOf(1, currentStage - 1)
                    Triple(newStage, now + reviewIntervals[newStage], 0)
                }
                currentStage >= LEARNED_STAGE -> {
                    Log.d(TAG, "Выученное слово забыто -> возврат на этап 7")
                    Triple(LEARNED_STAGE - 1, now + reviewIntervals[LEARNED_STAGE - 1], 1)
                }
                else -> Triple(currentStage, now + reviewIntervals[1], newFailCount)
            }
        }
    }

    override suspend fun getWordsFromFirebase(dictionaryId: String): List<WordEntity> {
        return withContext(Dispatchers.IO) {
            try {
                remoteSource.getWords(dictionaryId)
            } catch (e: Exception) {
                Log.e(TAG, "Error getting words from Firebase", e)
                emptyList()
            }
        }
    }
    // ФАЙЛ: main/java/com/example/t_learnappmobile/data/repository/WordRepositoryImpl.kt (добавить метод)
    override suspend fun clearUserProgress(userId: String) {
        withContext(Dispatchers.IO) {
            localSource.clearUserProgress(userId)
            Log.d(TAG, "Cleared user progress for: $userId")
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
            isNew = progress.stage == 0,
            userWordDocId = "${progress.userId}_${wordEntity.id}",
            failCount = progress.failCount
        )
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
    override suspend fun getUserProgress(userId: String, dictionaryId: String): List<UserWordEntity> {
        return withContext(Dispatchers.IO) {
            localSource.getUserProgress(userId, dictionaryId)
        }
    }
}
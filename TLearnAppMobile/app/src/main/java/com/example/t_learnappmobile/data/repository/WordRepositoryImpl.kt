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

    private  val LEARNED_STAGE = 8

    private val reviewIntervals = listOf(
        0L,                                    // stage 0 - новое слово
        5 * 60 * 1000L,                       // stage 1 - 5 минут
        10 * 60 * 1000L,                      // stage 2 - 10 минут
        60 * 60 * 1000L,                      // stage 3 - 1 час
        24 * 60 * 60 * 1000L,                 // stage 4 - 1 день
        7 * 24 * 60 * 60 * 1000L,             // stage 5 - 1 неделя
        30L * 24 * 60 * 60 * 1000,            // stage 6 - 1 месяц
        90L * 24 * 60 * 60 * 1000,            // stage 7 - 3 месяца
        Long.MAX_VALUE                        // stage 8 - выучено навсегда
    )

    override suspend fun loadWords(userId: String, dictionaryId: String): LoadWordsResult {
        Log.d(TAG, "Loading words for userId=$userId, dictionaryId=$dictionaryId")

        return try {
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
                loadFromRemote(userId, dictionaryId)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading words", e)
            LoadWordsResult.Error(e.message ?: "Неизвестная ошибка")
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

        return if (newStage < LEARNED_STAGE) {
            mapToDomain(wordEntity, updatedProgress)
        } else {
            null
        }
    }

    override suspend fun getStats(userId: String, dictionaryId: String): WordStats {
        val progress = localSource.getUserProgress(userId, dictionaryId)
        return WordStats(
            newWords = progress.count { it.stage == 0 },
            inProgressWords = progress.count { it.stage in 1 until LEARNED_STAGE },
            learnedWords = progress.count { it.stage >= LEARNED_STAGE }
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
        Log.d(TAG, "Reset dictionary progress for $dictionaryId")
    }

    override suspend fun resetAllProgress(userId: String) {
        val dictionaries = localSource.getDictionaries()
        dictionaries.forEach { dict ->
            resetDictionaryProgress(userId, dict.id)
        }
        Log.d(TAG, "Reset all progress for user $userId")
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
            // Логика для "знаю" - оставляем как было
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
            // НОВАЯ ЛОГИКА ДЛЯ "НЕ ЗНАЮ"
            val newFailCount = failCount + 1

            when {
                // Новое слово (этап 0)
                currentStage == 0 -> {
                    Log.d(TAG, "Новое слово, пользователь не знает -> этап 1")
                    Triple(1, now + reviewIntervals[1], newFailCount)
                }

                // Слова на этапах 1-7
                currentStage in 1 until LEARNED_STAGE -> {
                    when (newFailCount) {
                        1 -> {
                            // Первая ошибка: через 5 минут, этап не меняем
                            Log.d(TAG, "Этап $currentStage, 1-я ошибка -> покажем через 5 минут")
                            Triple(currentStage, now + reviewIntervals[1], newFailCount)
                        }
                        2 -> {
                            // Вторая ошибка: через 10 минут, этап не меняем
                            Log.d(TAG, "Этап $currentStage, 2-я ошибка -> покажем через 10 минут")
                            Triple(currentStage, now + reviewIntervals[2], newFailCount)
                        }
                        else -> {
                            // Третья и более ошибки: возвращаем на исходный этап, через 5 минут
                            Log.d(TAG, "Этап $currentStage, ${newFailCount}-я ошибка -> возврат на этап $currentStage через 5 минут")
                            Triple(currentStage, now + reviewIntervals[1], 0)  // Сбрасываем счетчик ошибок
                        }
                    }
                }

                // Выученное слово (этап 8 и выше)
                currentStage >= LEARNED_STAGE -> {
                    Log.d(TAG, "Выученное слово забыто -> возврат на этап 7")
                    Triple(LEARNED_STAGE - 1, now + reviewIntervals[LEARNED_STAGE - 1], 1)
                }

                else -> Triple(currentStage, now + reviewIntervals[1], newFailCount)
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
}
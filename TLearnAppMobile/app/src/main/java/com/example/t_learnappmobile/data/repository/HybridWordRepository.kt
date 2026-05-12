package com.example.t_learnappmobile.data.repository

import android.content.Context
import android.util.Log
import com.example.t_learnappmobile.data.local.AppDatabase
import com.example.t_learnappmobile.data.local.entities.*
import com.example.t_learnappmobile.data.sync.SyncManager
import com.example.t_learnappmobile.domain.repository.LoadWordsResult
import com.example.t_learnappmobile.domain.repository.WordRepository
import com.example.t_learnappmobile.model.*
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import kotlin.random.Random

class HybridWordRepository(
    private val context: Context,
    private val syncManager: SyncManager
) : WordRepository {

    private val database = AppDatabase.getInstance(context)
    private val wordDao = database.wordDao()
    private val firestore = Firebase.firestore
    private val auth = ServiceLocator.firebaseAuthManager
    private val TAG = "HybridWordRepo"

    private val _currentWord = MutableStateFlow<Word?>(null)
    override fun getCurrentWordFlow(): Flow<Word?> = _currentWord.asStateFlow()

    private val wordPool = ArrayList<Word>()
    private var currentDictionaryId = ""
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var isLoading = false
    private var autoRefreshJob: Job? = null

    private val reviewIntervals = listOf(
        0L, 5 * 60 * 1000L, 10 * 60 * 1000L, 60 * 60 * 1000L,
        24 * 60 * 60 * 1000L, 7 * 24 * 60 * 60 * 1000L,
        30L * 24 * 60 * 60 * 1000, 90L * 24 * 60 * 60 * 1000
    )

    private fun startAutoRefresh() {
        autoRefreshJob?.cancel()
        autoRefreshJob = scope.launch {
            while (isActive) {
                delay(30_000)
                refreshPoolIfNeeded()
            }
        }
        Log.d(TAG, "🔄 Auto-refresh started")
    }

    fun stopAutoRefresh() {
        autoRefreshJob?.cancel()
        autoRefreshJob = null
        Log.d(TAG, "🔄 Auto-refresh stopped")
    }

    private suspend fun refreshPoolIfNeeded() {
        if (wordPool.isEmpty() && currentDictionaryId.isNotEmpty()) {
            val userId = auth.getUserId() ?: return

            Log.d(TAG, "🔄 Pool is empty, checking for words to review...")

            try {
                val localWords = wordDao.getWordsByDictionary(currentDictionaryId)
                val localUserWords = wordDao.getUserWords(userId, currentDictionaryId)

                if (localWords.isNotEmpty() && localUserWords.isNotEmpty()) {
                    val wordsWithProgress = convertToWords(localWords, localUserWords)

                    if (wordsWithProgress.isNotEmpty()) {
                        wordPool.clear()
                        wordPool.addAll(wordsWithProgress)
                        showNextFromPool()
                        Log.d(TAG, "🔄 Refreshed pool with ${wordsWithProgress.size} words")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error refreshing pool", e)
            }
        }
    }

    override suspend fun loadWords(dictionaryId: String): LoadWordsResult {
        if (isLoading) {
            Log.d(TAG, "⚠️ Already loading, skipping duplicate call")
            return LoadWordsResult.HasWords
        }

        isLoading = true

        try {
            val userId = auth.getUserId()
            if (userId == null) {
                Log.e(TAG, "User not authenticated")
                return LoadWordsResult.Error("Пользователь не авторизован")
            }

            currentDictionaryId = dictionaryId
            Log.d(TAG, "📊 loadWords called for dict=$dictionaryId")

            val localWords = wordDao.getWordsByDictionary(dictionaryId)
            val localUserWords = wordDao.getUserWords(userId, dictionaryId)

            Log.d(TAG, "📊 Local: ${localWords.size} words, ${localUserWords.size} user words")

            for (uw in localUserWords) {
                Log.d(TAG, "  UserWord: wordId=${uw.wordId}, stage=${uw.stage}, isSynced=${uw.isSynced}")
            }

            if (localWords.isNotEmpty() && localUserWords.isNotEmpty()) {
                Log.d(TAG, "✅ Using local DB")
                val wordsWithProgress = convertToWords(localWords, localUserWords)

                wordPool.clear()
                wordPool.addAll(wordsWithProgress)

                if (wordsWithProgress.isNotEmpty()) {
                    showNextFromPool()
                    startAutoRefresh()
                    return LoadWordsResult.HasWords
                } else {
                    startAutoRefresh()
                    _currentWord.value = null
                    return LoadWordsResult.Empty
                }
            }

            if (localWords.isEmpty()) {
                Log.d(TAG, "📥 Loading words from Firebase...")
                val result = loadWordsFromFirebase(dictionaryId, userId)
                startAutoRefresh()
                return result
            }

            if (localUserWords.isEmpty()) {
                Log.d(TAG, "📥 Loading user progress from Firebase...")
                val result = loadUserProgressFromFirebase(dictionaryId, userId, localWords)
                startAutoRefresh()
                return result
            }

            Log.d(TAG, "⚠️ No words available")
            _currentWord.value = null
            startAutoRefresh()
            return LoadWordsResult.Empty
        } finally {
            isLoading = false
        }
    }

    private suspend fun loadUserProgressFromFirebase(
        dictionaryId: String,
        userId: String,
        existingWords: List<WordEntity>
    ): LoadWordsResult {
        try {
            Log.d(TAG, "📥 Loading user progress for ${existingWords.size} words")

            val userWordsSnapshot = firestore.collection("user_words")
                .whereEqualTo("userId", userId)
                .whereEqualTo("dictionaryId", dictionaryId)
                .get()
                .await()

            Log.d(TAG, "📥 Firebase returned ${userWordsSnapshot.documents.size} user_words")

            val now = System.currentTimeMillis()
            val newUserWords = ArrayList<UserWordEntity>()

            val firebaseDataMap = HashMap<String, Map<String, Any?>>()
            for (doc in userWordsSnapshot.documents) {
                val data = doc.data
                if (data != null) {
                    val wordId = data["wordId"] as? String
                    if (wordId != null) {
                        firebaseDataMap[wordId] = data
                    }
                }
            }

            for (word in existingWords) {
                val existingUserWord = wordDao.getUserWord(userId, word.id)

                if (existingUserWord != null) {
                    Log.d(TAG, "  Word ${word.id}: already exists with stage=${existingUserWord.stage}")
                    continue
                }

                val firebaseData = firebaseDataMap[word.id]

                val userWord = if (firebaseData != null) {
                    UserWordEntity(
                        userId = userId,
                        wordId = word.id,
                        dictionaryId = dictionaryId,
                        stage = (firebaseData["stage"] as? Long)?.toInt() ?: 0,
                        nextReviewDate = firebaseData["nextReviewDate"] as? Long ?: now,
                        failCount = (firebaseData["failCount"] as? Long)?.toInt() ?: 0,
                        lastReviewDate = firebaseData["lastReviewDate"] as? Long,
                        totalViews = (firebaseData["totalViews"] as? Long)?.toInt() ?: 0,
                        correctCount = (firebaseData["correctCount"] as? Long)?.toInt() ?: 0,
                        incorrectCount = (firebaseData["incorrectCount"] as? Long)?.toInt() ?: 0,
                        isSynced = true
                    )
                } else {
                    Log.d(TAG, "  Word ${word.id}: creating new progress")
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
                        isSynced = false
                    )
                }

                newUserWords.add(userWord)
                wordDao.insertUserWord(userWord)
                Log.d(TAG, "  ✅ Saved: wordId=${userWord.wordId}, stage=${userWord.stage}")
            }

            Log.d(TAG, "📊 Created ${newUserWords.size} new user words")

            val allUserWords = wordDao.getUserWords(userId, dictionaryId)
            Log.d(TAG, "📊 Total user words after update: ${allUserWords.size}")

            val wordsWithProgress = convertToWords(existingWords, allUserWords)
            Log.d(TAG, "📊 Words ready for pool: ${wordsWithProgress.size}")

            wordPool.clear()
            wordPool.addAll(wordsWithProgress)

            if (wordsWithProgress.isNotEmpty()) {
                showNextFromPool()
                return LoadWordsResult.HasWords
            } else {
                _currentWord.value = null
                return LoadWordsResult.Empty
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error loading user progress from Firebase", e)
            return LoadWordsResult.Error(e.message ?: "Ошибка загрузки прогресса")
        }
    }

    private suspend fun loadWordsFromFirebase(
        dictionaryId: String,
        userId: String
    ): LoadWordsResult {
        try {
            val wordsSnapshot = firestore.collection("words")
                .whereEqualTo("dictionaryId", dictionaryId)
                .get()
                .await()

            val words = ArrayList<WordEntity>()
            for (doc in wordsSnapshot.documents) {
                val data = doc.data
                if (data != null) {
                    words.add(
                        WordEntity(
                            id = doc.id,
                            dictionaryId = dictionaryId,
                            englishWord = data["englishWord"] as? String ?: "",
                            translation = data["translation"] as? String ?: "",
                            transcription = data["transcription"] as? String ?: "",
                            partOfSpeech = data["partOfSpeech"] as? String ?: ""
                        )
                    )
                }
            }

            if (words.isEmpty()) {
                return LoadWordsResult.Error("Нет слов в словаре")
            }

            wordDao.insertWords(words)

            val userWordsSnapshot = firestore.collection("user_words")
                .whereEqualTo("userId", userId)
                .whereEqualTo("dictionaryId", dictionaryId)
                .get()
                .await()

            val userWords = ArrayList<UserWordEntity>()
            for (doc in userWordsSnapshot.documents) {
                val data = doc.data
                if (data != null) {
                    val wordId = data["wordId"] as? String
                    if (wordId != null) {
                        userWords.add(
                            UserWordEntity(
                                userId = userId,
                                wordId = wordId,
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
                        )
                    }
                }
            }

            for (uw in userWords) {
                val existing = wordDao.getUserWord(userId, uw.wordId)
                if (existing == null || existing.updatedAt < uw.updatedAt) {
                    wordDao.insertUserWord(uw)
                }
            }

            val now = System.currentTimeMillis()
            val wordsWithProgress = ArrayList<Word>()

            for (wordEntity in words) {
                val userWord = userWords.find { it.wordId == wordEntity.id }
                if (userWord != null) {
                    val word = Word(
                        id = wordEntity.id,
                        dictionaryId = wordEntity.dictionaryId,
                        englishWord = wordEntity.englishWord,
                        translation = wordEntity.translation,
                        transcription = wordEntity.transcription,
                        partOfSpeech = parsePartOfSpeech(wordEntity.partOfSpeech),
                        stage = userWord.stage,
                        nextReviewDate = userWord.nextReviewDate,
                        isNew = userWord.stage == 0 && userWord.failCount == 0,
                        userWordDocId = "${userId}_${wordEntity.id}",
                        failCount = userWord.failCount
                    )
                    if (word.stage < 8 && word.nextReviewDate <= now) {
                        wordsWithProgress.add(word)
                    }
                }
            }

            wordPool.clear()
            wordPool.addAll(wordsWithProgress)

            if (wordsWithProgress.isNotEmpty()) {
                showNextFromPool()
                return LoadWordsResult.HasWords
            } else {
                _currentWord.value = null
                return LoadWordsResult.Empty
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error loading from Firebase", e)
            return LoadWordsResult.Error(e.message ?: "Ошибка загрузки слов")
        }
    }

    override suspend fun getDictionaries(): List<Dictionary> {
        try {
            val localDicts = wordDao.getDictionaries()
            if (localDicts.isNotEmpty()) {
                val result = ArrayList<Dictionary>()
                for (dict in localDicts) {
                    result.add(Dictionary(dict.id, dict.name, dict.order))
                }
                return result
            }

            val snapshot = firestore.collection("dictionaries")
                .orderBy("order")
                .get()
                .await()

            val dicts = ArrayList<Dictionary>()
            for (doc in snapshot.documents) {
                val data = doc.data
                if (data != null) {
                    dicts.add(
                        Dictionary(
                            id = doc.id,
                            name = data["name"] as? String ?: "",
                            order = (data["order"] as? Long)?.toInt() ?: 0
                        )
                    )
                }
            }

            val entities = ArrayList<DictionaryEntity>()
            for (dict in dicts) {
                entities.add(DictionaryEntity(dict.id, dict.name, dict.order))
            }
            wordDao.insertDictionaries(entities)

            return dicts
        } catch (e: Exception) {
            Log.e(TAG, "Error loading dictionaries", e)
            return listOf(
                Dictionary("finance", "Финансы", 1),
                Dictionary("conversational", "Разговорные слова", 2),
                Dictionary("technology", "Технологии", 3),
                Dictionary("slang", "Сленг", 4)
            )
        }
    }

    override fun answerWord(wordId: String, known: Boolean) {
        scope.launch {
            val userId = auth.getUserId() ?: return@launch

            val word = wordPool.find { it.id == wordId }
            if (word == null) {
                Log.e(TAG, "Word not found in pool: $wordId")
                return@launch
            }

            val now = System.currentTimeMillis()
            val (newStage, nextReviewDate, newFailCount) = calculateNextStage(
                word.stage, known, word.failCount, now
            )

            val existingUserWord = wordDao.getUserWord(userId, wordId)
            Log.d(TAG, "📝 Existing: ${existingUserWord?.stage}, New stage: $newStage")

            val updatedUserWord = UserWordEntity(
                userId = userId,
                wordId = wordId,
                dictionaryId = word.dictionaryId,
                stage = newStage,
                nextReviewDate = nextReviewDate,
                failCount = newFailCount,
                lastReviewDate = now,
                totalViews = (existingUserWord?.totalViews ?: 0) + 1,
                correctCount = (existingUserWord?.correctCount ?: 0) + (if (known) 1 else 0),
                incorrectCount = (existingUserWord?.incorrectCount ?: 0) + (if (known) 0 else 1),
                isSynced = false,
                updatedAt = now
            )

            wordDao.insertUserWord(updatedUserWord)

            val saved = wordDao.getUserWord(userId, wordId)
            Log.d(TAG, "✅ Saved to Room: stage=${saved?.stage}, isSynced=${saved?.isSynced}")

            val index = wordPool.indexOfFirst { it.id == wordId }
            if (index != -1) {
                wordPool[index] = word.copy(
                    stage = newStage,
                    nextReviewDate = nextReviewDate,
                    isNew = newStage == 0 && newFailCount == 0,
                    failCount = newFailCount
                )
            }

            Log.d(TAG, "Word $wordId: stage ${word.stage} -> $newStage")

            markCurrentWordAsShown()
            syncManager.syncPendingChanges()
        }
    }

    override fun markCurrentWordAsShown() {
        scope.launch {
            if (wordPool.isNotEmpty()) {
                wordPool.removeAt(0)
            }
            showNextFromPool()
        }
    }

    private suspend fun showNextFromPool() {
        if (wordPool.isEmpty()) {
            _currentWord.value = null
            Log.d(TAG, "⏳ No words available now, waiting for refresh...")


                delay(1000)
                refreshPoolIfNeeded()

            return
        }

        val nextWord = wordPool.first()
        val cardType = if (nextWord.stage == 0 && nextWord.isNew) CardType.NEW else CardType.ROTATION
        val direction = if (cardType == CardType.NEW) {
            TranslationDirection.EN_TO_RU
        } else {
            if (Random.nextBoolean()) TranslationDirection.EN_TO_RU
            else TranslationDirection.RU_TO_EN
        }

        _currentWord.value = nextWord.copy(
            translationDirection = direction,
            isNew = cardType == CardType.NEW
        )
        Log.d(TAG, "📖 Showing: '${nextWord.englishWord}' (stage=${nextWord.stage}, type=$cardType)")
    }

    private fun calculateNextStage(
        currentStage: Int,
        known: Boolean,
        failCount: Int,
        now: Long
    ): Triple<Int, Long, Int> {
        val maxStage = reviewIntervals.size - 1

        if (known) {
            if (currentStage == 0) {
                return Triple(8, Long.MAX_VALUE, 0)
            }
            if (currentStage < maxStage) {
                val newStage = currentStage + 1
                val interval = reviewIntervals[newStage]
                return Triple(newStage, now + interval, 0)
            }
            if (currentStage == maxStage) {
                return Triple(8, Long.MAX_VALUE, 0)
            }
        } else {
            val newFailCount = failCount + 1
            if (currentStage == 0) {
                return Triple(1, now + reviewIntervals[1], newFailCount)
            }
            if (currentStage >= 1 && newFailCount <= 2) {
                val retryInterval = if (newFailCount == 1) reviewIntervals[1] else reviewIntervals[2]
                return Triple(currentStage, now + retryInterval, newFailCount)
            }
            val newStage = maxOf(1, currentStage - 1)
            val interval = reviewIntervals[newStage]
            return Triple(newStage, now + interval, 0)
        }

        return Triple(currentStage, now, failCount)
    }

    private fun convertToWords(
        words: List<WordEntity>,
        userWords: List<UserWordEntity>
    ): List<Word> {
        val userWordsMap = userWords.associateBy { it.wordId }
        val now = System.currentTimeMillis()
        val result = ArrayList<Word>()

        for (wordEntity in words) {
            val userWord = userWordsMap[wordEntity.id] ?: continue
            val word = Word(
                id = wordEntity.id,
                dictionaryId = wordEntity.dictionaryId,
                englishWord = wordEntity.englishWord,
                translation = wordEntity.translation,
                transcription = wordEntity.transcription,
                partOfSpeech = parsePartOfSpeech(wordEntity.partOfSpeech),
                stage = userWord.stage,
                nextReviewDate = userWord.nextReviewDate,
                isNew = userWord.stage == 0 && userWord.failCount == 0,
                userWordDocId = "${userWord.userId}_${wordEntity.id}",
                failCount = userWord.failCount
            )

            if (word.stage < 8) {
                val shouldShow = word.isNew || word.nextReviewDate <= now
                if (shouldShow) {
                    result.add(word)
                }
            }
        }

        result.sortBy { word ->
            when {
                word.isNew -> 0
                word.nextReviewDate <= System.currentTimeMillis() -> 1
                else -> 2
            }
        }

        return result
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

    fun clearState() {
        scope.launch {
            wordPool.clear()
        }
        stopAutoRefresh()
        _currentWord.value = null
        currentDictionaryId = ""
    }
}
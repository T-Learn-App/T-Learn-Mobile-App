package com.example.t_learnappmobile.data.firebase

import android.util.Log
import com.example.t_learnappmobile.data.repository.ServiceLocator
import com.example.t_learnappmobile.domain.repository.LoadWordsResult
import com.example.t_learnappmobile.domain.repository.WordRepository
import com.example.t_learnappmobile.model.*
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import kotlin.random.Random

class FirebaseWordRepository : WordRepository {
    private val firestore = Firebase.firestore
    private val auth = ServiceLocator.firebaseAuthManager
    private val TAG = "FirebaseWordRepo"

    private val reviewIntervals = listOf(
        0L,                          // Этап 0: сразу
        5 * 60 * 1000L,             // Этап 1: 5 минут
        10 * 60 * 1000L,            // Этап 2: 10 минут
        60 * 60 * 1000L,            // Этап 3: 1 час
        24 * 60 * 60 * 1000L,       // Этап 4: 1 день
        7 * 24 * 60 * 60 * 1000L,   // Этап 5: 1 неделя
        30L * 24 * 60 * 60 * 1000,  // Этап 6: 1 месяц
        90L * 24 * 60 * 60 * 1000   // Этап 7: 3 месяца
    )

    private val _currentWord = MutableStateFlow<Word?>(null)
    override fun getCurrentWordFlow(): Flow<Word?> = _currentWord.asStateFlow()

    private val wordPool = mutableListOf<Word>()
    private var currentDictionaryId = ""
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private var autoCheckJob: Job? = null


    override suspend fun loadWords(dictionaryId: String): LoadWordsResult {
        val userId = auth.getUserId()
        if (userId == null) {
            Log.e(TAG, "User not authenticated")
            wordPool.clear()
            _currentWord.value = null
            return LoadWordsResult.Error("Пользователь не авторизован")
        }

        currentDictionaryId = dictionaryId
        wordPool.clear()

        return withContext(Dispatchers.IO) {
            try {
                val wordsSnapshot = firestore.collection("words")
                    .whereEqualTo("dictionaryId", dictionaryId)
                    .get()
                    .await()

                val allWords = wordsSnapshot.documents.mapNotNull { doc ->
                    val data = doc.data ?: return@mapNotNull null
                    Word(
                        id = doc.id,
                        dictionaryId = data["dictionaryId"] as? String ?: "",
                        englishWord = data["englishWord"] as? String ?: "",
                        translation = data["translation"] as? String ?: "",
                        transcription = data["transcription"] as? String ?: "",
                        partOfSpeech = parsePartOfSpeech(data["partOfSpeech"] as? String)
                    )
                }

                Log.d(TAG, "Found ${allWords.size} total words in dictionary")

                val now = System.currentTimeMillis()
                val wordsWithProgress = mutableListOf<Word>()

                for (word in allWords) {
                    val userWordDocId = "${userId}_${word.id}"
                    val userWordDoc = firestore.collection("user_words")
                        .document(userWordDocId)
                        .get()
                        .await()

                    if (userWordDoc.exists()) {
                        val data = userWordDoc.data ?: mapOf()
                        val stage = (data["stage"] as? Long)?.toInt() ?: 0
                        val nextReviewDate = (data["nextReviewDate"] as? Long) ?: now
                        val failCount = (data["failCount"] as? Long)?.toInt() ?: 0

                        wordsWithProgress.add(word.copy(
                            stage = stage,
                            nextReviewDate = nextReviewDate,
                            isNew = stage == 0 && failCount == 0,
                            userWordDocId = userWordDocId
                        ))
                    } else {
                        val initialData = mapOf(
                            "userId" to userId,
                            "wordId" to word.id,
                            "dictionaryId" to dictionaryId,
                            "stage" to 0,
                            "nextReviewDate" to now,
                            "lastReviewDate" to null,
                            "totalViews" to 0,
                            "correctCount" to 0,
                            "incorrectCount" to 0,
                            "failCount" to 0
                        )

                        firestore.collection("user_words")
                            .document(userWordDocId)
                            .set(initialData)
                            .await()

                        wordsWithProgress.add(word.copy(
                            stage = 0,
                            nextReviewDate = now,
                            isNew = true,
                            userWordDocId = userWordDocId
                        ))
                    }
                }

                val availableWords = wordsWithProgress
                    .filter { it.stage < 8 && it.nextReviewDate <= now }
                    .sortedBy { it.nextReviewDate }

                wordPool.clear()
                wordPool.addAll(availableWords)

                Log.d(TAG, "📊 Available: ${wordPool.size} | Total with progress: ${wordsWithProgress.size} | Learned: ${wordsWithProgress.count { it.stage >= 8 }}")

                showNextFromPool()
                startAutoCheck()


                return@withContext if (wordPool.isNotEmpty()) {
                    Log.d(TAG, "✅ HasWords — ${wordPool.size} words ready")
                    LoadWordsResult.HasWords
                } else {

                    val futureWords = wordsWithProgress.filter { it.stage in 1..7 && it.nextReviewDate > now }
                    if (futureWords.isNotEmpty()) {
                        Log.d(TAG, "⏳ Empty — ${futureWords.size} words scheduled for later")
                    } else {
                        Log.d(TAG, "🈳 Empty — no words at all")
                    }
                    _currentWord.value = null
                    LoadWordsResult.Empty}
            } catch (e: Exception) {
                Log.e(TAG, "Error loading words", e)
                _currentWord.value = null
                LoadWordsResult.Error(e.message ?: "Ошибка загрузки слов")
            }
        }
    }


    private fun startAutoCheck() {
        autoCheckJob?.cancel()
        autoCheckJob = scope.launch {
            Log.d(TAG, "Auto-check started for dictionary: $currentDictionaryId")
            while (isActive) {
                delay(15_000)

                val userId = auth.getUserId() ?: continue
                val dictId = currentDictionaryId

                if (dictId.isEmpty()) {
                    Log.d(TAG, "Auto-check: no dictionary selected, skipping")
                    continue
                }

                val now = System.currentTimeMillis()

                try {

                    val pendingWords = firestore.collection("user_words")
                        .whereEqualTo("userId", userId)
                        .whereEqualTo("dictionaryId", dictId)
                        .whereLessThanOrEqualTo("nextReviewDate", now)
                        .whereLessThan("stage", 8)
                        .get()
                        .await()

                    val pendingCount = pendingWords.documents.size

                    if (pendingCount > 0 && wordPool.isEmpty()) {
                        Log.d(TAG, "Auto-check: Found $pendingCount words ready, reloading...")
                        loadWords(dictId)
                    } else if (pendingCount > 0) {
                        Log.d(TAG, "Auto-check: $pendingCount words ready, but pool has ${wordPool.size} words")
                    } else {
                        Log.d(TAG, "Auto-check: No words ready yet")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Auto-check error", e)
                }
            }
        }
    }

    override suspend fun getDictionaries(): List<Dictionary> {
        return withContext(Dispatchers.IO) {
            try {
                val snapshot = firestore.collection("dictionaries")
                    .orderBy("order")
                    .get()
                    .await()

                snapshot.documents.mapNotNull { doc ->
                    val data = doc.data ?: return@mapNotNull null
                    Dictionary(
                        id = doc.id,
                        name = data["name"] as? String ?: "",
                        order = (data["order"] as? Long)?.toInt() ?: 0
                    )
                }
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

    override fun answerWord(wordId: String, known: Boolean) {
        scope.launch {
            val userId = auth.getUserId() ?: return@launch
            val userWordDocId = "${userId}_${wordId}"

            try {
                val docRef = firestore.collection("user_words").document(userWordDocId)
                val doc = docRef.get().await()

                if (!doc.exists()) {
                    Log.e(TAG, "Document not found: $userWordDocId")
                    return@launch
                }

                val data = doc.data ?: mapOf()
                val currentStage = (data["stage"] as? Long)?.toInt() ?: 0
                val failCount = (data["failCount"] as? Long)?.toInt() ?: 0
                val now = System.currentTimeMillis()

                val (newStage, nextReviewDate, newFailCount) = calculateNextStage(
                    currentStage, known, failCount, now
                )

                val updates = mapOf(
                    "stage" to newStage,
                    "nextReviewDate" to nextReviewDate,
                    "lastReviewDate" to now,
                    "totalViews" to FieldValue.increment(1),
                    "correctCount" to FieldValue.increment(if (known) 1 else 0),
                    "incorrectCount" to FieldValue.increment(if (known) 0 else 1),
                    "failCount" to newFailCount
                )

                docRef.update(updates).await()
                Log.d(TAG, "Word $wordId: stage $currentStage -> $newStage, nextReview in ${(nextReviewDate - now) / 1000}s")

                markCurrentWordAsShown()

            } catch (e: Exception) {
                Log.e(TAG, "Error updating word", e)
            }
        }
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
                // Новое слово + Знаю = сразу выучено
                return Triple(8, Long.MAX_VALUE, 0)
            } else if (currentStage < maxStage) {
                val newStage = currentStage + 1
                val interval = if (newStage < reviewIntervals.size) reviewIntervals[newStage] else Long.MAX_VALUE
                return Triple(newStage, now + interval, 0)
            } else if (currentStage == maxStage) {
                return Triple(8, Long.MAX_VALUE, 0)
            }
        } else {
            val newFailCount = failCount + 1

            if (currentStage == 0) {
                // Новое слово + Не знаю = этап 1
                return Triple(1, now + reviewIntervals[1], newFailCount)
            } else if (currentStage >= 1 && newFailCount <= 2) {
                val retryInterval = if (newFailCount == 1) reviewIntervals[1] else reviewIntervals[2]
                return Triple(currentStage, now + retryInterval, newFailCount)
            } else {
                val newStage = maxOf(1, currentStage - 1)
                val interval = if (newStage < reviewIntervals.size) reviewIntervals[newStage] else reviewIntervals[1]
                return Triple(newStage, now + interval, 0)
            }
        }

        return Triple(currentStage, now, failCount)
    }

    override fun markCurrentWordAsShown() {
        if (wordPool.isNotEmpty()) {
            wordPool.removeAt(0)
        }
        showNextFromPool()
    }

    private fun showNextFromPool() {
        if (wordPool.isEmpty()) {
            _currentWord.value = null
            Log.d(TAG, "No more words to show - pool is empty")
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

        val displayWord = nextWord.copy(
            translationDirection = direction,
            isNew = cardType == CardType.NEW
        )

        _currentWord.value = displayWord
        Log.d(TAG, "Showing: '${displayWord.englishWord}' (stage=${displayWord.stage}, type=$cardType, isNew=${displayWord.isNew})")
    }

    fun clearState() {
        Log.d(TAG, "Clearing repository state")
        autoCheckJob?.cancel()
        autoCheckJob = null
        wordPool.clear()
        _currentWord.value = null
        currentDictionaryId = ""
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
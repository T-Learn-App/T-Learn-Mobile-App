// Файл: data/firebase/FirebaseWordRepository.kt
package com.example.t_learnappmobile.data.firebase

import android.util.Log
import com.example.t_learnappmobile.data.repository.ServiceLocator
import com.example.t_learnappmobile.domain.repository.WordRepository
import com.example.t_learnappmobile.model.*
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlin.random.Random

class FirebaseWordRepository : WordRepository {
    private val firestore = Firebase.firestore
    private val auth = ServiceLocator.firebaseAuthManager
    private val TAG = "FirebaseWordRepo"

    // Интервалы для этапов (в миллисекундах)
    // Этап 0: новое слово (показывается сразу)
    // Этап 1: 5 минут
    // Этап 2: 1 час
    // Этап 3: 1 день
    // Этап 4: 1 неделя
    // Этап 5: 1 месяц
    // Этап 6: 3 месяца
    // Этап 7: выучено (больше не показываем)
    private val reviewIntervals = listOf(
        0L,                          // Этап 0: сразу
        5 * 60 * 1000L,             // Этап 1: 5 минут
        60 * 60 * 1000L,            // Этап 2: 1 час
        24 * 60 * 60 * 1000L,       // Этап 3: 1 день
        7 * 24 * 60 * 60 * 1000L,   // Этап 4: 1 неделя
        30L * 24 * 60 * 60 * 1000,  // Этап 5: 1 месяц
        90L * 24 * 60 * 60 * 1000   // Этап 6: 3 месяца
    )

    private val _currentWord = MutableStateFlow<Word?>(null)
    override fun getCurrentWordFlow(): Flow<Word?> = _currentWord.asStateFlow()

    private val wordPool = mutableListOf<Word>()
    private var currentDictionaryId = ""
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override suspend fun loadWords(dictionaryId: String) {
        val userId = auth.getUserId() ?: return
        currentDictionaryId = dictionaryId
        wordPool.clear()

        withContext(Dispatchers.IO) {
            try {
                // 1. Загружаем все слова словаря
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

                // 2. Загружаем прогресс пользователя
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
                            isNew = stage == 0,
                            userWordDocId = userWordDocId
                        ))
                    } else {
                        // Создаем запись для нового слова
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

                // 3. Фильтруем доступные слова (stage < 7 и время показа наступило)
                val availableWords = wordsWithProgress
                    .filter { it.stage < 7 && it.nextReviewDate <= now }
                    .sortedBy { it.nextReviewDate }

                wordPool.clear()
                wordPool.addAll(availableWords)

                Log.d(TAG, "Loaded ${wordPool.size} available words")
                showNextFromPool()

            } catch (e: Exception) {
                Log.e(TAG, "Error loading words", e)
                _currentWord.value = null
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
                Log.d(TAG, "Word $wordId: stage $currentStage -> $newStage, known=$known")

                // Удаляем текущее слово из пула и показываем следующее
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
        if (known) {
            // Нажата "Знаю"/"Запомнил"
            if (currentStage == 0) {
                // Новое слово -> этап 1 (5 минут)
                return Triple(1, now + reviewIntervals[1], 0)
            } else if (currentStage < 6) {
                // Повышаем этап
                return Triple(currentStage + 1, now + reviewIntervals[currentStage + 1], 0)
            } else if (currentStage == 6) {
                // Последний этап -> выучено
                return Triple(7, Long.MAX_VALUE, 0)
            }
        } else {
            // Нажата "Не знаю"/"Не запомнил"
            val newFailCount = failCount + 1

            if (currentStage == 0) {
                // Новое слово, которое не знают -> оставляем новым, покажем через 5 минут
                return Triple(0, now + reviewIntervals[1], newFailCount)
            } else if (currentStage >= 1 && newFailCount <= 2) {
                // Первые две ошибки -> повтор через 5 минут и 1 час
                val retryInterval = if (newFailCount == 1) reviewIntervals[1] else reviewIntervals[2]
                return Triple(currentStage, now + retryInterval, newFailCount)
            } else {
                // Третья ошибка -> возвращаем на предыдущий этап
                val newStage = maxOf(1, currentStage - 1)
                return Triple(newStage, now + reviewIntervals[newStage], 0)
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
            Log.d(TAG, "No more words to show")
            return
        }

        val nextWord = wordPool.first()
        val cardType = if (nextWord.stage == 0) CardType.NEW else CardType.ROTATION

        // Новые слова всегда EN->RU, в ротации - рандомно
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
        Log.d(TAG, "Showing: ${displayWord.englishWord} (stage: ${displayWord.stage}, type: $cardType)")
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
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

class FirebaseWordRepository : WordRepository {
    private val firestore = Firebase.firestore
    private val auth = ServiceLocator.firebaseAuthManager
    private val TAG = "FirebaseWordRepo"

    // Интервалы для этапов 1-6 (в миллисекундах)
    // stage 1: 5 минут
    // stage 2: 1 час
    // stage 3: 1 день (24 часа)
    // stage 4: 1 неделя
    // stage 5: 1 месяц (30 дней)
    // stage 6: 3 месяца (90 дней)
    private val reviewIntervals = listOf(
        5 * 60 * 1000L,              // 5 минут
        60 * 60 * 1000L,             // 1 час
        24 * 60 * 60 * 1000L,        // 1 день
        7 * 24 * 60 * 60 * 1000L,    // 1 неделя
        30 * 24 * 60 * 60 * 1000L,   // 1 месяц
        90 * 24 * 60 * 60 * 1000L    // 3 месяца
    )

    private val _currentWord = MutableStateFlow<Word?>(null)
    override fun getCurrentWordFlow(): Flow<Word?> = _currentWord.asStateFlow()

    // Пул слов, готовых к показу, отсортированный по приоритету
    private val wordPool = mutableListOf<Word>()

    // Кэш всех слов словаря (английское слово, перевод и т.д.)
    private var allWordsCache = mutableMapOf<String, Word>()

    private var currentDictionaryId = ""

    // Scope для фоновых операций
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override suspend fun loadWords(dictionaryId: String) {
        Log.d(TAG, "=== loadWords CALLED with dictionaryId: $dictionaryId ===")

        val userId = auth.getUserId()
        if (userId == null) {
            Log.e(TAG, "No user logged in!")
            return
        }

        currentDictionaryId = dictionaryId
        wordPool.clear()
        allWordsCache.clear()

        withContext(Dispatchers.IO) {
            try {
                // 1. Загружаем все слова из коллекции words для выбранного словаря
                Log.d(TAG, "Loading words from Firestore...")
                val wordsSnapshot = firestore.collection("words")
                    .whereEqualTo("dictionaryId", dictionaryId)
                    .get()
                    .await()

                Log.d(TAG, "Fetched ${wordsSnapshot.documents.size} words from 'words' collection")

                if (wordsSnapshot.documents.isEmpty()) {
                    Log.e(TAG, "NO WORDS FOUND in Firestore for dictionaryId: $dictionaryId")
                    _currentWord.value = null
                    return@withContext
                }

                // Кэшируем все слова
                val allDefinitions = wordsSnapshot.documents.mapNotNull { doc ->
                    val data = doc.data ?: return@mapNotNull null
                    Word(
                        id = doc.id,
                        dictionaryId = data["dictionaryId"] as? String ?: "",
                        englishWord = data["englishWord"] as? String ?: "",
                        translation = data["translation"] as? String ?: "",
                        transcription = data["transcription"] as? String ?: "",
                        partOfSpeech = parsePartOfSpeech(data["partOfSpeech"] as? String),
                        userWordDocId = "${userId}_${doc.id}"
                    )
                }

                allWordsCache = allDefinitions.associateBy { it.id }.toMutableMap()
                Log.d(TAG, "Cached ${allWordsCache.size} word definitions")

                // 2. Загружаем прогресс пользователя по всем словам этого словаря
                val allWordIds = allDefinitions.map { it.id }
                val userWordsData = loadUserWordsBatch(userId, allWordIds)

                // 3. Формируем полный список Word с учетом прогресса
                val now = System.currentTimeMillis()
                val allWordsWithProgress = mutableListOf<Word>()

                for (definition in allDefinitions) {
                    val progress = userWordsData[definition.id]

                    if (progress != null) {
                        val stage = (progress["stage"] as? Long)?.toInt() ?: 0
                        val nextReviewDate = (progress["nextReviewDate"] as? Long) ?: now

                        allWordsWithProgress.add(definition.copy(
                            stage = stage,
                            nextReviewDate = nextReviewDate,
                            isNew = stage == 0,
                            userWordDocId = "${userId}_${definition.id}"
                        ))
                    } else {
                        // Новое слово - создаем запись в user_words
                        val userWordDocId = "${userId}_${definition.id}"
                        val initialData = mapOf(
                            "userId" to userId,
                            "wordId" to definition.id,
                            "dictionaryId" to dictionaryId,
                            "stage" to 0,
                            "nextReviewDate" to now,
                            "lastReviewDate" to null,
                            "totalViews" to 0,
                            "correctCount" to 0,
                            "incorrectCount" to 0
                        )

                        firestore.collection("user_words")
                            .document(userWordDocId)
                            .set(initialData)
                            .await()

                        allWordsWithProgress.add(definition.copy(
                            stage = 0,
                            nextReviewDate = now,
                            isNew = true,
                            userWordDocId = userWordDocId
                        ))
                    }
                }

                // 4. Фильтруем: оставляем только те, у которых наступило время показа
                //    и которые не выучены (stage < 7)
                val availableWords = allWordsWithProgress
                    .filter { it.stage < 7 && it.nextReviewDate <= now }
                    .toMutableList()

                // 5. Сортируем:
                //    Приоритет 1: Слова в ротации (stage 1-6), сортировка по nextReviewDate (от ранних к поздним)
                //    Приоритет 2: Новые слова (stage 0), сортировка по nextReviewDate
                val rotationWords = availableWords
                    .filter { it.stage in 1..6 }
                    .sortedBy { it.nextReviewDate }

                val newWords = availableWords
                    .filter { it.stage == 0 }
                    .sortedBy { it.nextReviewDate }

                wordPool.clear()
                wordPool.addAll(rotationWords)
                wordPool.addAll(newWords)

                Log.d(TAG, "=== LOAD COMPLETE ===")
                Log.d(TAG, "Rotation words: ${rotationWords.size}")
                Log.d(TAG, "New words: ${newWords.size}")
                Log.d(TAG, "Total available: ${wordPool.size}")

                // Показываем первое слово из пула
                showNextFromPool()

            } catch (e: Exception) {
                Log.e(TAG, "Error loading words", e)
                _currentWord.value = null
            }
        }
    }

    private suspend fun loadUserWordsBatch(
        userId: String,
        wordIds: List<String>
    ): Map<String, Map<String, Any?>> {
        if (wordIds.isEmpty()) return emptyMap()

        // Firestore не поддерживает whereIn с >10 элементами, поэтому разбиваем на батчи
        val result = mutableMapOf<String, Map<String, Any?>>()
        val batchSize = 10

        for (i in wordIds.indices step batchSize) {
            val batch = wordIds.subList(i, minOf(i + batchSize, wordIds.size))
            val userWordDocIds = batch.map { "${userId}_$it" }

            if (userWordDocIds.isEmpty()) continue

            try {
                // Загружаем документы по ID
                for (docId in userWordDocIds) {
                    val doc = firestore.collection("user_words")
                        .document(docId)
                        .get()
                        .await()

                    if (doc.exists()) {
                        val data = doc.data ?: continue
                        val wordId = data["wordId"] as? String ?: continue
                        result[wordId] = data
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading batch", e)
            }
        }

        return result
    }

    override suspend fun getDictionaries(): List<Dictionary> {
        Log.d(TAG, "getDictionaries CALLED")
        return withContext(Dispatchers.IO) {
            try {
                val snapshot = firestore.collection("dictionaries")
                    .orderBy("order")
                    .get()
                    .await()

                Log.d(TAG, "Fetched ${snapshot.documents.size} dictionaries")

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
                emptyList()
            }
        }
    }

    override fun answerWord(userWordDocId: String, known: Boolean) {
        Log.d(TAG, "answerWord: userWordDocId=$userWordDocId, known=$known")

        // Запускаем в фоне через корутину
        scope.launch {
            val userId = auth.getUserId() ?: return@launch
            val now = System.currentTimeMillis()

            try {
                // Получаем текущий документ
                val docRef = firestore.collection("user_words").document(userWordDocId)
                val doc = docRef.get().await()

                if (!doc.exists()) {
                    Log.e(TAG, "Document $userWordDocId not found")
                    return@launch
                }

                val data = doc.data ?: return@launch
                val currentStage = (data["stage"] as? Long)?.toInt() ?: 0

                // Рассчитываем новый этап
                val newStage = calculateNewStage(currentStage, known)
                val nextReviewDate = calculateNextReviewDate(newStage, now)

                Log.d(TAG, "Stage: $currentStage -> $newStage, nextReview: $nextReviewDate")

                // Обновляем Firestore
                val updates = mapOf(
                    "stage" to newStage,
                    "nextReviewDate" to nextReviewDate,
                    "lastReviewDate" to now,
                    "totalViews" to FieldValue.increment(1),
                    "correctCount" to FieldValue.increment(if (known) 1 else 0),
                    "incorrectCount" to FieldValue.increment(if (known) 0 else 1)
                )

                docRef.update(updates).await()
                Log.d(TAG, "Word updated successfully")

            } catch (e: Exception) {
                Log.e(TAG, "Error updating word", e)
            }
        }
    }

    private fun calculateNewStage(currentStage: Int, known: Boolean): Int {
        return if (known) {
            // Нажата "Запомнил" / "Знаю"
            when {
                currentStage == 0 -> 1      // Новое слово переходит в ротацию
                currentStage < 6 -> currentStage + 1  // Повышаем этап
                currentStage == 6 -> 7      // Слово выучено!
                else -> currentStage        // stage >= 7, уже выучено
            }
        } else {
            // Нажата "Не запомнил" / "Не знаю"
            when {
                currentStage == 0 -> 0      // Новое слово остается новым
                currentStage == 1 -> 1      // Первый этап ротации - не опускаем ниже
                currentStage > 1 -> currentStage - 1  // Понижаем этап
                else -> currentStage
            }
        }
    }

    private fun calculateNextReviewDate(stage: Int, now: Long): Long {
        return when {
            stage >= 7 -> Long.MAX_VALUE  // Выучено - больше не показываем
            stage == 0 -> now             // Новое слово - можно показать сразу
            stage in 1..6 -> now + reviewIntervals[stage - 1]
            else -> now
        }
    }

    override fun markCurrentWordAsShown() {
        // Удаляем текущее слово из пула и показываем следующее
        if (wordPool.isNotEmpty()) {
            wordPool.removeAt(0)
        }
        showNextFromPool()
    }

    private fun showNextFromPool() {
        if (wordPool.isEmpty()) {
            _currentWord.value = null
            Log.d(TAG, "No more words to show!")
            return
        }

        val nextWord = wordPool.first()

        // Определяем тип карточки и направление перевода
        val cardType = if (nextWord.stage == 0) CardType.NEW else CardType.ROTATION
        val direction = if (cardType == CardType.NEW) {
            TranslationDirection.EN_TO_RU
        } else {
            // Для слов в ротации - рандомное направление
            if (kotlin.random.Random.nextBoolean()) {
                TranslationDirection.EN_TO_RU
            } else {
                TranslationDirection.RU_TO_EN
            }
        }

        val displayWord = nextWord.copy(
            translationDirection = direction,
            isNew = cardType == CardType.NEW
        )

        _currentWord.value = displayWord
        Log.d(TAG, "Showing word: ${displayWord.englishWord} (stage: ${displayWord.stage}, type: $cardType, direction: $direction)")
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
            else -> PartOfSpeech.NOUN
        }
    }
}
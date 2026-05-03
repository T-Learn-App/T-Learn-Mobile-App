package com.example.t_learnappmobile.data.firebase

import android.util.Log
import com.example.t_learnappmobile.data.repository.ServiceLocator
import com.example.t_learnappmobile.domain.repository.WordRepository
import com.example.t_learnappmobile.model.*
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class FirebaseWordRepository : WordRepository {
    private val firestore = Firebase.firestore
    private val auth = ServiceLocator.firebaseAuthManager
    private val TAG = "FirebaseWordRepo"

    private val reviewIntervals = listOf(
        5 * 60 * 1000L,
        60 * 60 * 1000L,
        24 * 60 * 60 * 1000L,
        7 * 24 * 60 * 60 * 1000L,
        30 * 24 * 60 * 60 * 1000L,
        90 * 24 * 60 * 60 * 1000L
    )

    private val _currentWord = MutableStateFlow<Word?>(null)
    override fun getCurrentWordFlow(): Flow<Word?> = _currentWord.asStateFlow()

    private var allUserWords = mutableListOf<Word>()
    private var currentWordIndex = -1

    init {
        Log.d(TAG, "FirebaseWordRepository CREATED")
    }

    override suspend fun loadWords(dictionaryId: String) {
        Log.d(TAG, "=== loadWords CALLED with dictionaryId: $dictionaryId ===")

        val userId = auth.getUserId()
        Log.d(TAG, "Current userId: $userId")

        if (userId == null) {
            Log.e(TAG, "No user logged in!")
            return
        }

        try {
            Log.d(TAG, "Loading words from Firestore...")

            val wordsSnapshot = firestore.collection("words")
                .whereEqualTo("dictionaryId", dictionaryId)
                .get()
                .await()

            Log.d(TAG, "Fetched ${wordsSnapshot.documents.size} documents from 'words' collection")

            if (wordsSnapshot.documents.isEmpty()) {
                Log.e(TAG, "NO WORDS FOUND in Firestore for dictionaryId: $dictionaryId")
                return
            }

            val allWords = wordsSnapshot.documents.mapNotNull { doc ->
                val data = doc.data ?: return@mapNotNull null
                Word(
                    id = doc.id,
                    dictionaryId = data["dictionaryId"] as? String ?: "",
                    englishWord = data["englishWord"] as? String ?: "",
                    translation = data["translation"] as? String ?: "",
                    transcription = data["transcription"] as? String ?: "",
                    partOfSpeech = parsePartOfSpeech(data["partOfSpeech"] as? String),
                )
            }

            Log.d(TAG, "Parsed ${allWords.size} words")

            val userProgressSnapshot = firestore.collection("user_words")
                .whereEqualTo("userId", userId)
                .get()
                .await()

            val userProgressMap = userProgressSnapshot.documents.associateBy({ it.id }, { it.data })

            allUserWords.clear()
            for (word in allWords) {
                val progress = userProgressMap[word.id]
                if (progress != null) {
                    val stage = (progress["stage"] as? Long)?.toInt() ?: 0
                    allUserWords.add(word.copy(
                        stage = stage,
                        nextReviewDate = progress["nextReviewDate"] as? Long ?: 0,
                        isNew = stage == 0
                    ))
                } else {
                    allUserWords.add(word.copy(
                        stage = 0,
                        nextReviewDate = System.currentTimeMillis(),
                        isNew = true
                    ))

                    firestore.collection("user_words").document(word.id).set(
                        mapOf(
                            "userId" to userId,
                            "wordId" to word.id,
                            "stage" to 0,
                            "nextReviewDate" to System.currentTimeMillis(),
                            "lastReviewDate" to null,
                            "totalViews" to 0,
                            "correctCount" to 0,
                            "incorrectCount" to 0
                        )
                    ).await()
                }
            }

            Log.d(TAG, "=== LOAD COMPLETE: ${allUserWords.size} words loaded ===")

            // Показываем первое слово
            currentWordIndex = 0
            if (allUserWords.isNotEmpty()) {
                _currentWord.value = allUserWords[currentWordIndex]
                Log.d(TAG, "Showing first word: ${allUserWords[currentWordIndex].englishWord}")
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error loading words", e)
        }
    }

    override suspend fun getDictionaries(): List<Dictionary> {
        Log.d(TAG, "getDictionaries CALLED")
        return try {
            val snapshot = firestore.collection("dictionaries").get().await()
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

    override fun answerWord(wordId: String, known: Boolean) {
        Log.d(TAG, "answerWord: wordId=$wordId, known=$known")

        CoroutineScope(Dispatchers.IO).launch {
            val userId = auth.getUserId() ?: return@launch
            val wordIndex = allUserWords.indexOfFirst { it.id == wordId }
            if (wordIndex == -1) return@launch

            val word = allUserWords[wordIndex]
            val now = System.currentTimeMillis()

            val newStage = if (known) {
                when {
                    word.isNew -> 1
                    word.stage < 7 -> word.stage + 1
                    else -> word.stage // уже выучено, оставляем
                }
            } else {
                when {
                    word.isNew -> 0
                    word.stage == 1 -> 1
                    else -> maxOf(1, word.stage - 1)
                }
            }

            val nextReviewDate = when {
                newStage >= 7 -> Long.MAX_VALUE
                newStage == 0 -> now + 5 * 60 * 1000
                newStage in 1..6 -> now + reviewIntervals[newStage - 1]
                else -> now
            }

            // Обновляем локально
            allUserWords[wordIndex] = word.copy(
                stage = newStage,
                nextReviewDate = nextReviewDate,
                isNew = newStage == 0
            )

            // Обновляем в Firestore
            val updates = mapOf(
                "stage" to newStage,
                "nextReviewDate" to nextReviewDate,
                "lastReviewDate" to now,
                "totalViews" to FieldValue.increment(1),
                "correctCount" to FieldValue.increment(if (known) 1 else 0),
                "incorrectCount" to FieldValue.increment(if (known) 0 else 1)
            )

            firestore.collection("user_words")
                .document(wordId)
                .update(updates)
                .await()

            Log.d(TAG, "Word ${word.englishWord} updated: stage $newStage")

            // Показываем следующее слово
            showNextWord()
        }
    }

    private fun showNextWord() {
        // Ищем следующее слово, которое готово к показу
        val now = System.currentTimeMillis()

        // Сначала новые слова
        val newWordIndex = allUserWords.indexOfFirst {
            it.isNew && it.stage == 0 && it.nextReviewDate <= now
        }

        if (newWordIndex != -1) {
            currentWordIndex = newWordIndex
            _currentWord.value = allUserWords[currentWordIndex]
            Log.d(TAG, "Next word (NEW): ${allUserWords[currentWordIndex].englishWord}")
            return
        }

        // Затем слова в ротации
        val rotationWordIndex = allUserWords.indexOfFirst {
            !it.isNew && it.stage in 1..6 && it.nextReviewDate <= now
        }

        if (rotationWordIndex != -1) {
            currentWordIndex = rotationWordIndex
            val word = allUserWords[currentWordIndex]
            // Рандомное направление для слов в ротации
            val randomDirection = if (kotlin.random.Random.nextBoolean()) {
                TranslationDirection.EN_TO_RU
            } else {
                TranslationDirection.RU_TO_EN
            }
            val updatedWord = word.copy(translationDirection = randomDirection)
            _currentWord.value = updatedWord
            Log.d(TAG, "Next word (ROTATION): ${word.englishWord}, direction: $randomDirection")
            return
        }

        // Если все слова выучены
        _currentWord.value = null
        Log.d(TAG, "No more words to show!")
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
            else -> PartOfSpeech.NOUN
        }
    }
}
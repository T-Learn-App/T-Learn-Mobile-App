package com.example.t_learnappmobile.data.repository

import android.util.Log
import com.example.t_learnappmobile.domain.model.RepetitionSchedule
import com.example.t_learnappmobile.domain.repository.WordRepository
import com.example.t_learnappmobile.model.CardType
import com.example.t_learnappmobile.model.TranslationDirection
import com.example.t_learnappmobile.model.Word
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import java.time.LocalDateTime
import kotlin.random.Random

class WordRepositoryImpl : WordRepository {
    private val wordsStorage = mutableListOf<Word>().apply {
        add(createNewWord("Card", "карточка, открытка, билет", "[kārd]", "Noun"))
        add(createNewWord("Book", "книга, учебник", "[bʊk]", "Noun"))
        add(createNewWord("House", "дом, здание", "[haʊs]", "Noun"))
        add(createNewWord("School", "школа", "[skuːl]", "Noun"))
        add(createNewWord("Teacher", "учитель", "[ˈtiːtʃər]", "Noun"))
    }

    private val _wordsUpdate = MutableStateFlow(0L)

    override fun getCurrentCardFlow(): Flow<Word?> = _wordsUpdate
        .map { getCurrentCard() }

    override fun getCurrentCard(): Word? {
        val now = LocalDateTime.now()

        val allRotationCards = wordsStorage.filter {
            it.cardType == CardType.ROTATION && !it.isLearned
        }
        allRotationCards.forEach { card ->
            val diff = java.time.Duration.between(now, card.nextRepetitionTime).toMinutes()
        }

        val readyRotationCards = wordsStorage.filter {
            it.cardType == CardType.ROTATION &&
                    !it.isLearned &&
                    it.nextRepetitionTime <= now
        }


        if (readyRotationCards.isNotEmpty()) {
            val earliestReadyCard = readyRotationCards.minByOrNull { it.nextRepetitionTime }
            val direction = if (Random.nextBoolean()) {
                TranslationDirection.ENGLISH_TO_RUSSIAN
            } else {
                TranslationDirection.RUSSIAN_TO_ENGLISH
            }
            return earliestReadyCard?.copy(translationDirection = direction)
        }

        val newCard = wordsStorage.firstOrNull {
            it.cardType == CardType.NEW && !it.isLearned
        }
        if (newCard != null) {
            return newCard.copy(translationDirection = TranslationDirection.ENGLISH_TO_RUSSIAN)
        }

        return null
    }


    override fun getNewWords(): List<Word> {
        return wordsStorage.filter { it.cardType == CardType.NEW && !it.isLearned }
    }

    override fun getRotationWords(): List<Word> {
        return wordsStorage.filter { it.cardType == CardType.ROTATION && !it.isLearned }
    }

    override fun getLearnedWords(): List<Word> {
        return wordsStorage.filter { it.isLearned }
    }

    override fun getCardReadyForRepetition(): List<Word> {
        val now = LocalDateTime.now()
        return wordsStorage.filter {
            it.cardType == CardType.ROTATION &&
                    !it.isLearned &&
                    it.nextRepetitionTime <= now
        }
    }

    override fun markAsSuccessful(word: Word) {
        val index = wordsStorage.indexOfFirst { it.id == word.id }
        if (index != -1) {
            val newStage = RepetitionSchedule.getNextStage(word.repetitionStage)
            val newCardType = if (word.cardType == CardType.NEW) CardType.ROTATION else word.cardType
            val updatedWord = word.copy(
                cardType = newCardType,
                repetitionStage = newStage,
                originalRepetitionStage = newStage,
                nextRepetitionTime = RepetitionSchedule.getNextRepetitionTime(newStage),
                failureAttempts = 0,
                isLearned = word.repetitionStage >= 7,
                pendingFailureStage = null,
                pendingFailureRepeats = null
            )
            wordsStorage[index] = updatedWord
            triggerUpdate()
        }
    }

    override fun markAsFailure(word: Word) {
        val index = wordsStorage.indexOfFirst { it.id == word.id }
        if (index != -1) {
            if (word.cardType == CardType.NEW) {
                val rotationWord = word.copy(
                    cardType = CardType.ROTATION,
                    repetitionStage = 0,
                    originalRepetitionStage = 0,
                    nextRepetitionTime = RepetitionSchedule.getNextRepetitionTime(0)
                )
                wordsStorage[index] = rotationWord
                markAsFailure(rotationWord)
                return
            }

            if (word.pendingFailureRepeats == null) {
                val updatedWord = word.copy(
                    nextRepetitionTime = LocalDateTime.now().plusMinutes(5),
                    pendingFailureRepeats = 2,
                    pendingFailureStage = word.repetitionStage,
                    failureAttempts = word.failureAttempts + 1
                )
                wordsStorage[index] = updatedWord
            } else if (word.pendingFailureRepeats == 2) {
                val updatedWord = word.copy(
                    nextRepetitionTime = LocalDateTime.now().plusHours(1),
                    pendingFailureRepeats = 1,
                    failureAttempts = word.failureAttempts + 1
                )
                wordsStorage[index] = updatedWord
            } else if (word.pendingFailureRepeats == 1) {
                val originalStage = word.pendingFailureStage ?: 0
                val updatedWord = word.copy(
                    repetitionStage = originalStage,
                    nextRepetitionTime = RepetitionSchedule.getNextRepetitionTime(originalStage),
                    pendingFailureRepeats = null,
                    pendingFailureStage = null,
                    failureAttempts = word.failureAttempts + 1
                )
                wordsStorage[index] = updatedWord
            } else {
                val updatedWord = word.copy(
                    cardType = CardType.ROTATION,
                    repetitionStage = 0,
                    nextRepetitionTime = RepetitionSchedule.getFailureRepetitionTime(0),
                    failureAttempts = word.failureAttempts + 1,
                    pendingFailureRepeats = null,
                    pendingFailureStage = null
                )
                wordsStorage[index] = updatedWord
            }
            triggerUpdate()
        }
    }

    override fun moveToRotation(word: Word) {
        val index = wordsStorage.indexOfFirst { it.id == word.id }
        if (index != -1) {
            val updatedWord = word.copy(
                cardType = CardType.ROTATION,
                repetitionStage = 0,
                originalRepetitionStage = 0,
                nextRepetitionTime = RepetitionSchedule.getNextRepetitionTime(0)
            )
            wordsStorage[index] = updatedWord
            triggerUpdate()
        }
    }

    override fun addWord(word: Word) {
        wordsStorage.add(word)
        triggerUpdate()
    }

    override fun triggerUpdate() {
        _wordsUpdate.value = System.currentTimeMillis()
    }

    private fun createNewWord(
        english: String,
        russian: String,
        transcription: String,
        partOfSpeech: String
    ): Word {
        return Word(
            id = "${english}_${System.currentTimeMillis()}",
            englishWord = english,
            russianTranslation = russian,
            transcription = transcription,
            partOfSpeech = partOfSpeech,
            category = "Простые слова",
            cardType = CardType.NEW
        )
    }
}

package com.example.t_learnappmobile.model

import java.time.LocalDateTime

enum class CardType {
    NEW,
    ROTATION
}

enum class TranslationDirection {
    ENGLISH_TO_RUSSIAN,
    RUSSIAN_TO_ENGLISH
}
data class Word (
    val id: String,
    val englishWord: String,
    val transcription: String,
    val partOfSpeech: String,
    val russianTranslation: String,
    val category: String,

    val cardType: CardType = CardType.NEW,
    val repetitionStage: Int = 0,
    val originalRepetitionStage: Int = 0,
    val isLearned: Boolean = false,

    val createdAt: LocalDateTime = LocalDateTime.now(),
    val nextRepetitionTime: LocalDateTime = LocalDateTime.now().plusYears(10),
    val failureAttempts: Int = 0,

    val translationDirection: TranslationDirection = TranslationDirection.ENGLISH_TO_RUSSIAN,

    val pendingFailureRepeats: Int? = null,
    val pendingFailureStage: Int? = null

)
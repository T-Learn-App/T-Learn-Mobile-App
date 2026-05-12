// domain/model/Word.kt
package com.example.t_learnappmobile.domain.model

data class Word(
    val id: String,
    val dictionaryId: String = "",
    val englishWord: String = "",
    val translation: String = "",
    val transcription: String = "",
    val partOfSpeech: PartOfSpeech = PartOfSpeech.UNKNOWN,
    val stage: Int = 0,
    val nextReviewDate: Long = 0,
    val isNew: Boolean = true,
    val translationDirection: TranslationDirection = TranslationDirection.EN_TO_RU,
    val userWordDocId: String = "",
    val failCount: Int = 0
)

data class Dictionary(
    val id: String,
    val name: String,
    val order: Int = 0
)

data class WordProgress(
    val userId: String,
    val wordId: String,
    val dictionaryId: String,
    val stage: Int,
    val nextReviewDate: Long,
    val failCount: Int = 0,
    val totalViews: Int = 0,
    val correctCount: Int = 0,
    val incorrectCount: Int = 0,
    val isSynced: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
)

data class WordStats(
    val newWords: Int = 0,
    val inProgressWords: Int = 0,
    val learnedWords: Int = 0
)

enum class CardType {
    NEW,
    ROTATION
}

enum class TranslationDirection {
    EN_TO_RU,
    RU_TO_EN
}

enum class PartOfSpeech(val displayName: String) {
    NOUN("существительное"),
    VERB("глагол"),
    ADJECTIVE("прилагательное"),
    ADVERB("наречие"),
    PRONOUN("местоимение"),
    PREPOSITION("предлог"),
    CONJUNCTION("союз"),
    INTERJECTION("междометие"),
    UNKNOWN("неизвестно")
}
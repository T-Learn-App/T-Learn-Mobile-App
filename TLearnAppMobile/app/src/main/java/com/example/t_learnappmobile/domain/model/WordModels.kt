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
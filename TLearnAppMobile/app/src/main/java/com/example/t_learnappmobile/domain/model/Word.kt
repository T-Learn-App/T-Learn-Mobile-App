package com.example.t_learnappmobile.model

enum class CardType {
    NEW,        // Новое слово (еще не в ротации)
    ROTATION    // Слово в ротации
}

enum class TranslationDirection {
    EN_TO_RU,   // Английский -> Русский
    RU_TO_EN    // Русский -> Английский
}

enum class PartOfSpeech(val russian: String) {
    NOUN("существительное"),
    VERB("глагол"),
    ADJECTIVE("прилагательное"),
    ADVERB("наречие"),
    PRONOUN("местоимение"),
    PREPOSITION("предлог"),
    CONJUNCTION("союз"),
    INTERJECTION("междометие")
}

data class Word(
    val id: String,
    val dictionaryId: String,
    val englishWord: String,
    val translation: String,
    val transcription: String,
    val partOfSpeech: PartOfSpeech,
    val stage: Int = 0,              // 0-8, 8 = выучено
    val nextReviewDate: Long = 0,    // когда показывать
    val isNew: Boolean = true,       // новое слово
    val translationDirection: TranslationDirection = TranslationDirection.EN_TO_RU
)
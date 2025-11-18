package com.example.t_learnappmobile.model

enum class CardType {
    NEW,
    ROTATION
}

enum class TranslationDirection {
    ENGLISH_TO_RUSSIAN,
    RUSSIAN_TO_ENGLISH
}

enum class PartOfSpeech() {
    NOUN("Существительное"),
    ADJECTIVE("Прилагательное"),
    VERB("Глагол"),
    PRONOUN("Местоимение"),
    ADVERB("Наречие");

    lateinit var russianName: String
    constructor(russian: String) : this() { this.russianName = russian}
}
data class Word (
    val id: Int,
    val vocabularyId: Int,
    val englishWord: String,
    val transcription: String,
    val partOfSpeech: PartOfSpeech,
    val russianTranslation: String,


    val category: String,

    val cardType: CardType = CardType.NEW,
    val repetitionStage: Int = 0,
    val isLearned: Boolean = false,
    val translationDirection: TranslationDirection = TranslationDirection.ENGLISH_TO_RUSSIAN,
)

data class VocabularyStats(
    val vocabularyId: Int,

    // может быть обьединить два поля ниже
    val alreadyKnown: Int,
    val memorized: Int,

    val inProgress: Int
)
package com.example.t_learnappmobile.model

enum class CardType {
    NEW,
    ROTATION
}

enum class TranslationDirection {
    ENGLISH_TO_RUSSIAN,
    RUSSIAN_TO_ENGLISH
}

enum class PartOfSpeech {
    NOUN, ADJECTIVE, VERB, PRONOUN, INTERJECTION, ADVERB;

    val russianName: String
        get() = when (this) {
            NOUN -> "Существительное"
            ADJECTIVE -> "Прилагательное"
            VERB -> "Глагол"
            PRONOUN -> "Местоимение"
            INTERJECTION -> "Междометие"
            ADVERB -> "Наречие"
        }
}

data class Word(
    val id: Long,
    val vocabularyId: Int,
    val englishWord: String,
    val transcription: String,
    val partOfSpeech: PartOfSpeech,
    val russianTranslation: String,
    val category: String,
    val cardType: CardType = CardType.NEW,
    val repetitionStage: Int = 0,
    val isLearned: Boolean = false,
    val translationDirection: TranslationDirection = TranslationDirection.ENGLISH_TO_RUSSIAN
)
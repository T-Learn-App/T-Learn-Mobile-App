package com.example.t_learnappmobile.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.t_learnappmobile.model.CardType
import com.example.t_learnappmobile.model.PartOfSpeech
import com.example.t_learnappmobile.model.TranslationDirection
import com.example.t_learnappmobile.model.Word

@Entity(tableName = "words")
data class WordEntity(
    @PrimaryKey
    val id: Long,
    val vocabularyId: Int,
    val englishWord: String,
    val transcription: String,
    val partOfSpeech: String,
    val russianTranslation: String,
    val category: String,
    val cardType: String,
    val repetitionStage: Int = 0,
    val isLearned: Boolean = false,
    val translationDirection: String
) {
    fun toWord(): Word {
        return Word(
            id = id,
            vocabularyId = vocabularyId,
            englishWord = englishWord,
            transcription = transcription,
            partOfSpeech = PartOfSpeech.valueOf(partOfSpeech),
            russianTranslation = russianTranslation,
            category = category,
            cardType = CardType.valueOf(cardType),
            repetitionStage = repetitionStage,
            isLearned = isLearned,
            translationDirection = TranslationDirection.valueOf(translationDirection)
        )
    }

    companion object {
        fun fromWord(word: Word): WordEntity {
            return WordEntity(
                id = word.id,
                vocabularyId = word.vocabularyId,
                englishWord = word.englishWord,
                transcription = word.transcription,
                partOfSpeech = word.partOfSpeech.name,
                russianTranslation = word.russianTranslation,
                category = word.category,
                cardType = word.cardType.name,
                repetitionStage = word.repetitionStage,
                isLearned = word.isLearned,
                translationDirection = word.translationDirection.name
            )
        }
    }
}
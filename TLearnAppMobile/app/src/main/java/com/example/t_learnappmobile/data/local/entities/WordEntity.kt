package com.example.t_learnappmobile.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "words")
data class WordEntity(
    @PrimaryKey val id: String,
    val dictionaryId: String,
    val englishWord: String,
    val translation: String,
    val transcription: String,
    val partOfSpeech: String,
    val lastUpdated: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "user_words",
    primaryKeys = ["userId", "wordId"]
)
data class UserWordEntity(
    val userId: String,
    val wordId: String,
    val dictionaryId: String,
    val stage: Int,
    val nextReviewDate: Long,
    val failCount: Int,
    val lastReviewDate: Long?,
    val totalViews: Int,
    val correctCount: Int,
    val incorrectCount: Int,
    val isSynced: Boolean = true,  // Для отслеживания несинхронизированных изменений
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "dictionaries")
data class DictionaryEntity(
    @PrimaryKey val id: String,
    val name: String,
    val order: Int
)
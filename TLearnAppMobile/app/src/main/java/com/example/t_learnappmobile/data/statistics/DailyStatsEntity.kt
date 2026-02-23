package com.example.t_learnappmobile.data.statistics

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_stats")
data class DailyStatsEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Int,
    val dictionaryId: Int,
    val date: String,
    val newWords: Int = 0,
    val inProgressWords: Int = 0,
    val learnedWords: Int = 0
)
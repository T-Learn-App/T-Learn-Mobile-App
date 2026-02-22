package com.example.t_learnappmobile.data.game

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.t_learnappmobile.domain.model.GameResult

@Entity(tableName = "game_results")
data class GameResultEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: Int,
    val score: Int,
    val wordsCount: Int,
    val timestamp: Long
)

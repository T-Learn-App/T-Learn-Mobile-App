// domain/model/GameModels.kt
package com.example.t_learnappmobile.domain.model

data class GameWord(
    val id: Long,
    val english: String,
    val russian: String
)

data class GameResult(
    val score: Int,
    val totalWords: Int,
    val timestamp: Long = System.currentTimeMillis()
)

data class DailyStats(
    val date: String,
    val learnedWords: Int = 0,
    val gamesPlayed: Int = 0,
    val totalScore: Int = 0
)

data class LeaderboardPlayer(
    val id: String,
    val name: String,
    val score: Int,
    val avatarUrl: String? = null,
    val position: Int = 0
)
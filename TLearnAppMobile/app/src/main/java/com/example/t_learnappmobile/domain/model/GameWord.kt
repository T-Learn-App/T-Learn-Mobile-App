package com.example.t_learnappmobile.domain.model

data class GameWord(
    val id: Long,
    val english: String,
    val russian: String
)

data class GameResult(
    val score: Int,
    val wordsCount: Int,
    val timestamp: Long = System.currentTimeMillis()
)

enum class GameMode { TIME, WORDS }

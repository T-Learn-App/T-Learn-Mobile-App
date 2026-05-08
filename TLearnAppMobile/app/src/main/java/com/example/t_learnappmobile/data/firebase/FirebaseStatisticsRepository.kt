package com.example.t_learnappmobile.data.firebase


data class DailyStats(
    val date: String,
    val learnedWords: Int = 0,
    val gamesPlayed: Int = 0,
    val totalScore: Int = 0
)
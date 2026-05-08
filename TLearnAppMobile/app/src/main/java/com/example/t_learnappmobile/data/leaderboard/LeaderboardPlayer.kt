package com.example.t_learnappmobile.data.leaderboard



data class LeaderboardPlayer(
    val id: String,  // ← Изменено с Int на String
    val name: String,
    val score: Int,
    val avatarUrl: String? = null,
    val position: Int = 0
)
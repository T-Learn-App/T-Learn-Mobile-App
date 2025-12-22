
package com.example.t_learnappmobile.data.statistics


data class DailyStats(
    val date: String,
    val newWords: Int = 0,
    val inProgressWords: Int = 0,
    val learnedWords: Int = 0
)

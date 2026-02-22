package com.example.t_learnappmobile.presentation.game

import com.example.t_learnappmobile.domain.model.GameWord
import com.example.t_learnappmobile.domain.model.GameMode

data class GameState(
    val currentWord: GameWord? = null,
    val score: Int = 0,
    val timer: Int = 120,
    val wordsLeft: Int = 15,
    val currentWordIndex: Int = 0,
    val totalWords: Int = 15,
    val gameMode: GameMode = GameMode.TIME,
    val isGameActive: Boolean = false,
    val options: List<String> = emptyList(),
    val correctOptionIndex: Int = 0
)

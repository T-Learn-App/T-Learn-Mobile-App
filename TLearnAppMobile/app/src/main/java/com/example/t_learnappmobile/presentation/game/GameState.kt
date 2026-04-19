package com.example.t_learnappmobile.presentation.game

import com.example.t_learnappmobile.domain.model.GameWord
import com.example.t_learnappmobile.domain.model.GameMode

data class GameState(
    val currentWord: GameWord? = null,
    val score: Int = 0,
    val wordsLeft: Int = 10,
    val currentWordIndex: Int = 0,
    val totalWords: Int = 10,
    val gameMode: GameMode = GameMode.WORDS,
    val isGameActive: Boolean = false,
    val options: List<String> = emptyList(),
    val correctOptionIndex: Int = 0,
    val showResults: Boolean = false
)

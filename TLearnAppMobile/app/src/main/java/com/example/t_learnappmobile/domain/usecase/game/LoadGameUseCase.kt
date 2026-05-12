// domain/usecase/game/LoadGameWordsUseCase.kt
package com.example.t_learnappmobile.domain.usecase.game

import com.example.t_learnappmobile.domain.model.GameWord
import com.example.t_learnappmobile.domain.repository.GameRepository

class LoadGameWordsUseCase(
    private val gameRepository: GameRepository
) {
    suspend operator fun invoke(dictionaryId: String, limit: Int = 10): List<GameWord> {
        return gameRepository.loadGameWords(dictionaryId, limit)
    }
}
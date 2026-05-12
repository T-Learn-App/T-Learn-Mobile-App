// domain/usecase/words/GetWordStatsUseCase.kt
package com.example.t_learnappmobile.domain.usecase.words

import com.example.t_learnappmobile.domain.model.WordStats
import com.example.t_learnappmobile.domain.repository.WordRepository

class GetWordStatsUseCase(
    private val wordRepository: WordRepository
) {
    suspend operator fun invoke(userId: String, dictionaryId: String): WordStats {
        return wordRepository.getStats(userId, dictionaryId)
    }
}
// domain/usecase/words/ProcessAnswerUseCase.kt
package com.example.t_learnappmobile.domain.usecase.words

import com.example.t_learnappmobile.domain.model.Word
import com.example.t_learnappmobile.domain.repository.WordRepository

class ProcessAnswerUseCase(
    private val wordRepository: WordRepository
) {
    suspend operator fun invoke(
        userId: String,
        wordId: String,
        dictionaryId: String,
        known: Boolean
    ): Word? {
        return wordRepository.processAnswer(userId, wordId, dictionaryId, known)
    }
}
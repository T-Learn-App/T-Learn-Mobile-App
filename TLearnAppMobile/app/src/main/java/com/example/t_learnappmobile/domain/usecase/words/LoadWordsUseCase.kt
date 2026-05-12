// domain/usecase/words/LoadWordsUseCase.kt
package com.example.t_learnappmobile.domain.usecase.words

import com.example.t_learnappmobile.domain.model.Word
import com.example.t_learnappmobile.domain.repository.LoadWordsResult
import com.example.t_learnappmobile.domain.repository.WordRepository

class LoadWordsUseCase(
    private val wordRepository: WordRepository
) {
    suspend operator fun invoke(userId: String, dictionaryId: String): LoadWordsResult {
        return wordRepository.loadWords(userId, dictionaryId)
    }
}
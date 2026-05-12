// domain/usecase/words/GetDictionariesUseCase.kt
package com.example.t_learnappmobile.domain.usecase.words

import com.example.t_learnappmobile.domain.model.Dictionary
import com.example.t_learnappmobile.domain.repository.WordRepository

class GetDictionariesUseCase(
    private val wordRepository: WordRepository
) {
    suspend operator fun invoke(): List<Dictionary> {
        return wordRepository.getDictionaries()
    }
}
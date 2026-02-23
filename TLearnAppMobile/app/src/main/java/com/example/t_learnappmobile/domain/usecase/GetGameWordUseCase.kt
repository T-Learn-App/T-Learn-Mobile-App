package com.example.t_learnappmobile.domain.usecase

import com.example.t_learnappmobile.domain.model.GameWord
import com.example.t_learnappmobile.domain.repository.WordRepository

class GetGameWordsUseCase(private val repository: WordRepository) {
    suspend operator fun invoke(): List<GameWord> {
        val words = repository.getNewWords() + repository.getRotationWords()
        return words.map {
            GameWord(it.id, it.englishWord, it.russianTranslation)
        }
    }
}

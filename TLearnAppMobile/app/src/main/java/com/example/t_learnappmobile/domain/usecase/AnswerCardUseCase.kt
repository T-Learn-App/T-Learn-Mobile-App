package com.example.t_learnappmobile.domain.usecase

import com.example.t_learnappmobile.domain.repository.WordRepository
import com.example.t_learnappmobile.model.CardType
import com.example.t_learnappmobile.model.Word

class AnswerCardUseCase(private val repository: WordRepository) {
    fun executeSuccess(word: Word) {
        when(word.cardType){
            CardType.ROTATION -> {
                repository.moveToRotation(word)
            }
            CardType.NEW -> {
                repository.markAsSuccessful(word)
            }

        }
    }
    fun executeFailure(word: Word){
        repository.markAsFailure(word)
    }
}
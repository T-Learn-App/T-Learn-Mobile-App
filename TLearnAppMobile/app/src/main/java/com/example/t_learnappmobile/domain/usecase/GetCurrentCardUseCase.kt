package com.example.t_learnappmobile.domain.usecase

import com.example.t_learnappmobile.domain.repository.WordRepository
import com.example.t_learnappmobile.model.Word

class GetCurrentCardUseCase(private val repository: WordRepository) {
    fun execute() : Word? {
        return repository.getCurrentCard()
    }
}
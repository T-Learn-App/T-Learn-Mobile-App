// Файл: domain/repository/WordRepository.kt
package com.example.t_learnappmobile.domain.repository

import com.example.t_learnappmobile.model.Dictionary
import com.example.t_learnappmobile.model.Word
import kotlinx.coroutines.flow.Flow

interface WordRepository {
    suspend fun loadWords(dictionaryId: String)
    suspend fun getDictionaries(): List<Dictionary>
    fun answerWord(wordId: String, known: Boolean)
    fun getCurrentWordFlow(): Flow<Word?>
    fun markCurrentWordAsShown()
}
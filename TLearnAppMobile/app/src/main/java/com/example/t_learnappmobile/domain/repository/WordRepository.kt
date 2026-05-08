package com.example.t_learnappmobile.domain.repository

import com.example.t_learnappmobile.model.Dictionary
import com.example.t_learnappmobile.model.Word
import kotlinx.coroutines.flow.Flow

sealed class LoadWordsResult {
    object HasWords : LoadWordsResult()
    object Empty : LoadWordsResult()
    data class Error(val message: String) : LoadWordsResult()
}

interface WordRepository {
    suspend fun loadWords(dictionaryId: String): LoadWordsResult  // ИЗМЕНЕНО: возвращает результат
    suspend fun getDictionaries(): List<Dictionary>
    fun answerWord(wordId: String, known: Boolean)
    fun getCurrentWordFlow(): Flow<Word?>
    fun markCurrentWordAsShown()
}
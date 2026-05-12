// domain/repository/WordRepository.kt
package com.example.t_learnappmobile.domain.repository

import com.example.t_learnappmobile.domain.model.Dictionary
import com.example.t_learnappmobile.domain.model.Word
import com.example.t_learnappmobile.domain.model.WordStats

sealed class LoadWordsResult {
    data class HasWords(val words: List<Word>) : LoadWordsResult()
    object Empty : LoadWordsResult()
    data class Error(val message: String) : LoadWordsResult()
}

interface WordRepository {
    suspend fun loadWords(userId: String, dictionaryId: String): LoadWordsResult
    suspend fun getDictionaries(): List<Dictionary>
    suspend fun processAnswer(userId: String, wordId: String, dictionaryId: String, known: Boolean): Word?
    suspend fun getStats(userId: String, dictionaryId: String): WordStats
    suspend fun resetDictionaryProgress(userId: String, dictionaryId: String)
    suspend fun resetAllProgress(userId: String)
}
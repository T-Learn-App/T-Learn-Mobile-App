package com.example.t_learnappmobile.domain.repository

import com.example.t_learnappmobile.data.local.entities.UserWordEntity
import com.example.t_learnappmobile.data.local.entities.WordEntity
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
    suspend fun resetDictionaryProgressAndSync(userId: String, dictionaryId: String)
    suspend fun resetAllProgressAndSync(userId: String)
    suspend fun getUserProgress(userId: String, dictionaryId: String): List<UserWordEntity>
    suspend fun getWordsFromFirebase(dictionaryId: String): List<WordEntity>
    suspend fun clearUserProgress(userId: String)
}
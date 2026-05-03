package com.example.t_learnappmobile.domain.repository

import com.example.t_learnappmobile.model.Word
import kotlinx.coroutines.flow.Flow
import com.example.t_learnappmobile.model.Dictionary

interface WordRepository {
    fun answerWord(wordId: String, known: Boolean)
    suspend fun loadWords(dictionaryId: String)
    suspend fun getDictionaries(): List<Dictionary>
    fun getCurrentWordFlow(): Flow<Word?>
}
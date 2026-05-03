package com.example.t_learnappmobile.domain.repository

import com.example.t_learnappmobile.model.Dictionary
import com.example.t_learnappmobile.model.Word
import kotlinx.coroutines.flow.Flow

interface WordRepository {
    suspend fun loadWords(dictionaryId: String)
    suspend fun getDictionaries(): List<Dictionary>
    fun answerWord(userWordDocId: String, known: Boolean)
    fun getCurrentWordFlow(): Flow<Word?>
    fun markCurrentWordAsShown()
}
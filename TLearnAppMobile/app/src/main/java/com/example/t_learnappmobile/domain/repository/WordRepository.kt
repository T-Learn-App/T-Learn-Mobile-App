package com.example.t_learnappmobile.domain.repository

import com.example.t_learnappmobile.domain.model.CardAction
import com.example.t_learnappmobile.model.VocabularyStats
import com.example.t_learnappmobile.model.Word
import kotlinx.coroutines.flow.Flow

interface WordRepository {
    fun getCurrentCardFlow(): Flow<Word?>
    fun getCurrentCard(): Word?
    fun getNewWords(): List<Word>
    fun getLearnedWords(): List<Word>
    fun getRotationWords(): List<Word>
    fun addWord(word: Word)

    suspend fun fetchWordBatch(userId: Int, vocabularyId: Int, batchSize: Int = 10): List<Word>?
    suspend fun sendRotationAction(wordId: Int, action: CardAction): Boolean
    suspend fun fetchStats(vocabularyId: Int) : VocabularyStats?
    fun nextWord()

}
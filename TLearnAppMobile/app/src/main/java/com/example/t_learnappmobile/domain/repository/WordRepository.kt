
package com.example.t_learnappmobile.domain.repository

import com.example.t_learnappmobile.domain.model.CardAction
import com.example.t_learnappmobile.model.Word
import kotlinx.coroutines.flow.Flow

interface WordRepository {
    fun nextWord()
    fun getCurrentCardFlow(): Flow<Word?>
    fun getCurrentCard(): Word?


    fun getNewWords(): List<Word>
    fun getRotationWords(): List<Word>
    fun getLearnedWords(): List<Word>

    fun addWord(word: Word)
    suspend fun fetchWords(categoryId: Long): List<Word>
    suspend fun completeWord(wordId: Long, action: CardAction): Boolean
}
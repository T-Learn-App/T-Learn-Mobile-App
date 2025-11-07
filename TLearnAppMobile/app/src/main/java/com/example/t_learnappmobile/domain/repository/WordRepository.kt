package com.example.t_learnappmobile.domain.repository

import android.provider.UserDictionary
import com.example.t_learnappmobile.model.Word
import kotlinx.coroutines.flow.Flow

interface WordRepository {
    fun getCurrentCardFlow(): Flow<Word?>
    fun getCurrentCard(): Word?
    fun getNewWords(): List<Word>
    fun getRotationWords(): List<Word>
    fun getLearnedWords(): List<Word>
    fun getCardReadyForRepetition():List<Word>
    fun markAsSuccessful(word: Word)
    fun markAsFailure(word: Word)
    fun moveToRotation(word: Word)
    fun addWord(word: Word)
    fun triggerUpdate()
}
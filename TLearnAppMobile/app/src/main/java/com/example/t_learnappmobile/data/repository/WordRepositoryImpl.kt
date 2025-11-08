package com.example.t_learnappmobile.data.repository

import com.example.t_learnappmobile.domain.repository.WordRepository
import com.example.t_learnappmobile.model.PartOfSpeech
import com.example.t_learnappmobile.model.VocabularyStats
import com.example.t_learnappmobile.model.Word
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map


class WordRepositoryImpl : WordRepository {
    private val wordsStorage = mutableListOf<Word>().apply {
        add(Word(1, 1, "Hello", "[hɛˈloʊ]", PartOfSpeech.NOUN, "Привет", "english_basic"))
    }
    private val _wordsUpdate = MutableStateFlow(0L)
    override fun getCurrentCardFlow(): Flow<Word?> = _wordsUpdate.map { getCurrentCard() }


    override fun getCurrentCard(): Word? {
        return wordsStorage.firstOrNull()
    }

    override fun getNewWords(): List<Word> {
        TODO("Not yet implemented")
    }

    override fun getLearnedWords(): List<Word> {
        TODO("Not yet implemented")
    }

    override fun addWord(word: Word) {
        TODO("Not yet implemented")
    }

    override fun triggerUpdate() {
        _wordsUpdate.value = System.currentTimeMillis()
    }

    override suspend fun fetchWordBatch(
        vocabularyId: Int,
        batchSize: Int
    ): List<Word>? {
        TODO("Not yet implemented")
    }

    override suspend fun sendRotationAction(
        wordId: Int,
        action: String
    ): Boolean {
        TODO("Not yet implemented")
    }

    override suspend fun fetchStats(vocabularyId: Int): VocabularyStats? {
        TODO("Not yet implemented")
    }


}

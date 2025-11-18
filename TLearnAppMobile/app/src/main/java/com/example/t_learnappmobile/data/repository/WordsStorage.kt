package com.example.t_learnappmobile.data.repository

import com.example.t_learnappmobile.model.CardType
import com.example.t_learnappmobile.model.Word
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class WordsStorage {
    private val _wordsFlow = MutableStateFlow<List<Word>>(emptyList())
    val wordsFlow: StateFlow<List<Word>> = _wordsFlow

    private val _currentWordIndex = MutableStateFlow(0)
    val currentWordIndex: StateFlow<Int> = _currentWordIndex

    val currentCardFlow: Flow<Word?> = combine(_wordsFlow, _currentWordIndex) { words, index ->
        val word = if (index in words.indices) words[index] else null
        word
    }


    fun updateWords(words: List<Word>) {
        _wordsFlow.value = words
        _currentWordIndex.value = 0
    }

    fun getCurrentWord(): Word? {
        val words = _wordsFlow.value
        val index = _currentWordIndex.value
        return if (index < words.size) words[index] else null
    }

    fun getNewWords(): List<Word> {
        return _wordsFlow.value.filter { it.cardType == CardType.NEW }
    }

    fun getLearnedWords(): List<Word> {
        return _wordsFlow.value.filter { it.isLearned }
    }

    fun getRotationWords(): List<Word> {
        return _wordsFlow.value.filter { it.cardType == CardType.ROTATION }
    }

    fun addWord(word: Word) {
        val currentList = _wordsFlow.value.toMutableList()
        currentList.add(word)
        _wordsFlow.value = currentList
    }

    fun nextWord() {
        if (_currentWordIndex.value < _wordsFlow.value.size - 1) {
            _currentWordIndex.value++
        }
    }


}

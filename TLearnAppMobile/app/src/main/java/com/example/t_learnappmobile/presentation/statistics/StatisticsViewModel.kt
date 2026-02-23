package com.example.t_learnappmobile.presentation.statistics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.t_learnappmobile.data.leaderboard.LeaderboardManager
import com.example.t_learnappmobile.data.leaderboard.LeaderboardPlayer
import com.example.t_learnappmobile.data.repository.ServiceLocator
import com.example.t_learnappmobile.data.statistics.DailyStats
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class StatisticsViewModel : ViewModel() {
    private val dictionaryManager = ServiceLocator.dictionaryManager
    private val tokenManager = ServiceLocator.tokenManager
    private val leaderboardManager = LeaderboardManager()

    private val _weekStats = MutableStateFlow<List<DailyStats>>(emptyList())
    val weekStats: StateFlow<List<DailyStats>> = _weekStats

    private val _totalStats = MutableStateFlow(TotalStats(0, 0, 0))
    val totalStats: StateFlow<TotalStats> = _totalStats

    private val _currentDictionaryName = MutableStateFlow("Conversational")
    val currentDictionaryName: StateFlow<String> = _currentDictionaryName


    private val _leaderboardPlayers = MutableStateFlow<List<LeaderboardPlayer>>(emptyList())
    val leaderboardPlayers: StateFlow<List<LeaderboardPlayer>> = _leaderboardPlayers

    private val _yourPosition = MutableStateFlow<LeaderboardPlayer?>(null)
    val yourPosition: StateFlow<LeaderboardPlayer?> = _yourPosition
    private val _seasonText = MutableStateFlow("Сезон 1 (22.02-22.03)")
    val seasonText: StateFlow<String> = _seasonText

    init {
        loadWeekStats()
        loadLeaderboard()
        viewModelScope.launch {
            dictionaryManager.currentVocabularyIdFlow
                .filterNotNull()
                .distinctUntilChanged()
                .collect { loadWeekStats() }
        }
    }

    fun loadWeekStats() {
        viewModelScope.launch {
            val userId = tokenManager.getUserData().firstOrNull()?.id ?: return@launch
            val currentDict = dictionaryManager.getCurrentDictionary(userId)

            _currentDictionaryName.value = currentDict.name
            val totalWordsInDict = currentDict.wordsCount
            val totalLearned = dictionaryManager.getTotalLearnedWords(userId)
            val newWordsTotal = totalWordsInDict - totalLearned

            val stats = dictionaryManager.getLastWeekStats(userId)
            val updatedStats = stats.map { daily ->
                daily.copy(newWords = newWordsTotal)
            }

            _weekStats.value = updatedStats
            _totalStats.value = TotalStats(
                new = newWordsTotal,
                inProgress = stats.sumOf { it.inProgressWords },
                learned = stats.sumOf { it.learnedWords }
            )
        }
    }

    private fun loadLeaderboard() {
        viewModelScope.launch {
            val userId = tokenManager.getUserData().firstOrNull()?.id ?: 1

            val topPlayers = leaderboardManager.loadLeaderboard()
            _leaderboardPlayers.value = topPlayers

            val yourPosition = leaderboardManager.getYourPosition(userId)
            _yourPosition.value = yourPosition
        }
    }


    data class TotalStats(val new: Int, val inProgress: Int, val learned: Int)
}

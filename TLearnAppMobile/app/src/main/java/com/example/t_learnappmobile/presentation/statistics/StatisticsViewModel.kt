package com.example.t_learnappmobile.presentation.statistics


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.t_learnappmobile.data.repository.ServiceLocator
import com.example.t_learnappmobile.data.statistics.DailyStats
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class StatisticsViewModel : ViewModel() {

    private val dictionaryManager = ServiceLocator.dictionaryManager

    private val _weekStats = MutableStateFlow<List<DailyStats>>(emptyList())
    val weekStats: StateFlow<List<DailyStats>> = _weekStats

    private val _totalStats = MutableStateFlow(TotalStats(0, 0, 0))
    val totalStats: StateFlow<TotalStats> = _totalStats

    private val _weekLabel = MutableStateFlow("")
    val weekLabel: StateFlow<String> = _weekLabel

    private val tokenManager = ServiceLocator.tokenManager
    init {
        loadWeekStats()
    }

    fun loadWeekStats() {
        viewModelScope.launch {
            val userId = tokenManager.getUserData().firstOrNull()?.id ?: return@launch
            val stats = dictionaryManager.getLastWeekStats(userId)
            _weekStats.value = stats

            val total = stats.fold(TotalStats(0, 0, 0)) { acc, d ->
                TotalStats(
                    acc.new + d.newWords,
                    acc.inProgress + d.inProgressWords,
                    acc.learned + d.learnedWords
                )
            }
            _totalStats.value = total
            _weekLabel.value = dictionaryManager.getWeekLabel(userId)
        }
    }

    data class TotalStats(val new: Int, val inProgress: Int, val learned: Int)
}

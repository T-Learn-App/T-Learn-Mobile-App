package com.example.t_learnappmobile.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class EmailVerificationViewModel : ViewModel() {
    private val TIMER_DURATION = 60000L
    private val TICK_INTERVAL = 1000L

    private val _timerText = MutableStateFlow<String>("")
    val timerText: StateFlow<String> = _timerText

    private val _isResendEnabled = MutableStateFlow(false)
    val isResendEnabled: StateFlow<Boolean> = _isResendEnabled

    private val _isTimerActive = MutableStateFlow(false)
    val isTimeActive: StateFlow<Boolean> = _isTimerActive

    private var timerJob: Job? = null
    private var timerRemaining = 0L

    fun startTimer() {
        timerJob?.cancel()
        timerRemaining = TIMER_DURATION
        _isResendEnabled.value = false

        timerJob = viewModelScope.launch {
            while (isActive && timerRemaining > 0) {
                val secondsRemaining = timerRemaining / 1000
                _timerText.value = "Повторить через: $secondsRemaining s"
                delay(TICK_INTERVAL)
                timerRemaining -= TICK_INTERVAL
            }
            _timerText.value = "Код истек"
        }
    }

    fun resetCode() {
        startTimer()
    }

    override fun onCleared() {
        timerJob?.cancel()
        super.onCleared()
    }

}
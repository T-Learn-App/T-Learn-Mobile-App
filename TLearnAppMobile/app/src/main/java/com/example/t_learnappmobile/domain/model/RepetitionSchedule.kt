package com.example.t_learnappmobile.domain.model

import com.example.t_learnappmobile.domain.model.RepetitionSchedule.intervals
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import kotlin.compareTo

object RepetitionSchedule {
    private val intervals = listOf(
        5,
        60,
        24 * 60,
        7 * 24 * 60,
        30 * 24 * 60,
        90 * 24 * 60,
        365 * 24 * 60,
        Int.MAX_VALUE
    )
    fun getFailureRepetitionTime(failureAttempts: Int) : LocalDateTime {
        return when (failureAttempts){
            0 -> LocalDateTime.now().plus(5, ChronoUnit.MINUTES)
            1 -> LocalDateTime.now().plus(60, ChronoUnit.MINUTES)
            else -> LocalDateTime.now().plus(5, ChronoUnit.MINUTES)
        }
    }
    fun getNextRepetitionTime(stage: Int) : LocalDateTime {
        return if (stage >= intervals.size){
            LocalDateTime.now().plus(365, ChronoUnit.DAYS)
        } else {
            LocalDateTime.now().plus(intervals[stage].toLong(), ChronoUnit.MINUTES)
        }
    }

    fun getNextStage(stage: Int): Int {
        return (stage + 1).coerceAtMost(7)
    }

    fun restoreOriginalRepetitionTime(originalStage: Int): LocalDateTime {
        return getNextRepetitionTime(originalStage)
    }
}
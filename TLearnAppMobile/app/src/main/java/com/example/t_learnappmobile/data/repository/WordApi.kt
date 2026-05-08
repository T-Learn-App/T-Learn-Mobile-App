package com.example.t_learnappmobile.data.repository

import com.example.t_learnappmobile.domain.model.ListWordResponse
import retrofit2.Response
import retrofit2.http.*

data class CompleteWordRequest(
    val wordId: Long
)

data class ListStatsDto(
    val stats: List<StatDto>?
)

data class StatDto(
    val userId: Long,
    val wordId: Long,
    val attempts: Long,
    val status: String,
    val lastDays: Long? = null
)

data class StatsFilterRequest(
    val lastDays: Long
)


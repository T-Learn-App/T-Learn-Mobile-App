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

interface WordApi {
    @GET("words")
    suspend fun getAllWords(
        @Header("Authorization") authorization: String
    ): Response<ListWordResponse>

    @GET("words/categories/{categoryId}")
    suspend fun getWordsByCategory(
        @Header("Authorization") authorization: String,
        @Path("categoryId") categoryId: Long
    ): Response<ListWordResponse>

    @GET("stats")
    suspend fun getStats(
        @Header("Authorization") authorization: String
    ): Response<ListStatsDto>

    @POST("stats")
    suspend fun getStatsByDays(
        @Header("Authorization") authorization: String,
        @Body filter: StatsFilterRequest
    ): Response<ListStatsDto>

    @PUT("stats/complete")
    suspend fun completeWord(
        @Header("Authorization") authorization: String,
        @Body request: CompleteWordRequest
    ): Response<Unit>
}
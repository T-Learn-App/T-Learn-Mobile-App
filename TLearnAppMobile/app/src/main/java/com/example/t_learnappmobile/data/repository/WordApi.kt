package com.example.t_learnappmobile.data.repository

import ListWordResponse

import retrofit2.Response
import retrofit2.http.*

data class StatsResponse(
    val newWords: Int,
    val inProgressWords: Int,
    val learnedWords: Int
)

data class CompleteStatsRequest(
    val wordId: Long,
    val isCorrect: Boolean
)

interface WordApi {
    @GET("words/categories/{categoryId}")
    suspend fun getWordsByCategory(
        @Header("Authorization") authorization: String,
        @Path("categoryId") categoryId: Long
    ): Response<ListWordResponse>

    @GET("stats")
    suspend fun getStats(
        @Header("Authorization") authorization: String
    ): Response<StatsResponse>

    @PUT("stats/complete")
    suspend fun completeWord(
        @Header("Authorization") authorization: String,
        @Body request: CompleteStatsRequest
    ): Response<Unit>

    @GET("words")
    suspend fun getAllWords(
        @Header("Authorization") authorization: String
    ): Response<ListWordResponse>
}
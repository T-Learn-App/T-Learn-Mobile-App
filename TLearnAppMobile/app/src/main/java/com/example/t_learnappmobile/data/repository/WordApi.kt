package com.example.t_learnappmobile.data.repository

import ListWordResponse
import StatQueueDto


import retrofit2.Response
import retrofit2.http.*

interface WordApi {

    @GET("words")
    suspend fun getWords(
        @Header("Authorization") authorization: String
    ): Response<ListWordResponse>

    @GET("words/categories/{categoryId}")
    suspend fun getWordsByCategory(
        @Header("Authorization") authorization: String,
        @Path("categoryId") categoryId: Long
    ): Response<ListWordResponse>  // ✅ ListWordResponse (с words: List<WordResponse>)


    @PUT("stats/complete")
    suspend fun completeWord(
        @Header("Authorization") authorization: String,
        @Body dto: StatQueueDto
    ): Response<Unit>
}
package com.example.t_learnappmobile.data.repository

import com.example.t_learnappmobile.domain.model.ListWordResponse
import com.example.t_learnappmobile.domain.model.StatQueueDto
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
    ): Response<ListWordResponse>

    @PUT("stats/complete")
    suspend fun completeWord(
        @Header("Authorization") authorization: String,
        @Body dto: StatQueueDto
    ): Response<Unit>
}
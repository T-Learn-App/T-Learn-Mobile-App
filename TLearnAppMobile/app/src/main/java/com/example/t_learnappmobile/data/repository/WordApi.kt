package com.example.t_learnappmobile.data.repository

import com.example.t_learnappmobile.domain.model.RotationAction
import com.example.t_learnappmobile.model.Word
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface WordApi {
    @GET("words")
    suspend fun getBatch(
        @Query("vocabulary_id") vocabularyId: Int,
        @Query("batch_size") batchSize: Int
    ): Response<List<Word>>

    @POST("rotation")
    suspend fun sendAction(@Body action: RotationAction): Response<Unit>
}
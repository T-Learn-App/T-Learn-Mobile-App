// domain/model/WordResponse.kt
package com.example.t_learnappmobile.domain.model

import com.google.gson.annotations.SerializedName

data class ListWordResponse(
    val userId: Long,
    val words: List<WordResponse>
)

data class WordResponse(
    @SerializedName("id")
    val id: Long,

    @SerializedName("word")
    val word: String,

    @SerializedName("transcription")
    val transcription: String,

    @SerializedName("translation")
    val translation: String,

    @SerializedName("partOfSpeech")
    val partOfSpeech: String,

    @SerializedName("category")
    val category: Long
)
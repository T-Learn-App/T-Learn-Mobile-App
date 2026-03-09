import com.google.gson.annotations.SerializedName

// package com.example.t_learnappmobile.domain.model
data class ListWordResponse(
    val words: List<WordResponse>  // ← УБРАЛ userId!
)

data class WordResponse(
    val id: Long,
    @SerializedName("part_of_speech")
    val partOfSpeech: String,      // ← snake_case → camelCase
    @SerializedName("category_id")
    val category: Long,            // ← snake_case → camelCase
    val word: String,
    val transcription: String,
    val translation: String? = "перевод"  // ← default значение
)

data class StatQueueDto(
    val wordId: Long
)

package com.example.t_learnappmobile.domain.model

data class WordResponse(
    val id: Long,
    val word: String,
    val transcription: String,
    val translation: String,
    val partOfSpeech: String,
    val category: Long
)

data class ListWordResponse(
    val userId: Long,
    val words: List<WordResponse>
)

data class StatQueueDto(
    val wordId: Long
)

data class AuthResponse(
    val accessToken: String,
    val refreshToken: String
)
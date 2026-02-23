package com.example.t_learnappmobile.data.dictionary

data class Dictionary(
    val id: Int,
    val vocabularyId: Int,
    val name: String,
    val description: String,
    val wordsCount: Int = 0
)

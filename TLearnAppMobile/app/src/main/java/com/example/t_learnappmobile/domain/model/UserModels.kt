// domain/model/UserModels.kt
package com.example.t_learnappmobile.domain.model

data class UserProfile(
    val uid: String,
    val email: String?,
    val firstName: String = "",
    val lastName: String = "",
    val totalScore: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)
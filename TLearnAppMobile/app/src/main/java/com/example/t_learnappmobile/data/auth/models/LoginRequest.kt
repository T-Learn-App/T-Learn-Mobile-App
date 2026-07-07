package com.example.t_learnappmobile.data.auth.models

data class LoginRequest(
    val email: String,
    val password: String,
    val accessToken: String? = null,
    val refreshToken: String? = null
)
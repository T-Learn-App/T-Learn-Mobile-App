package com.example.t_learnappmobile.data.auth.models

data class AuthResponse(
    val accessToken: String,
    val refreshToken: String? = null

)
package com.example.t_learnappmobile.data.auth.models

import com.google.gson.annotations.SerializedName

data class UserData(
    @SerializedName("id")
    val id: String,
    @SerializedName("email")
    val email: String,
    @SerializedName("login")
    val login: String,
    @SerializedName("created_at")
    val createdAt: String? = null,
    @SerializedName("is_verified")
    val isVerified: Boolean = false
)

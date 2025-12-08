package com.example.t_learnappmobile.data.auth.models

import com.google.gson.annotations.SerializedName

data class EmailVerificationResponse(
    @SerializedName("message")
    val message: String,
    @SerializedName("expires_in")
    val expiresIn: Int = 300
)

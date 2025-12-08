package com.example.t_learnappmobile.data.auth.models

import com.google.gson.annotations.SerializedName

data class EmailVerificationRequest(
    @SerializedName("email")
    val email: String,
    @SerializedName("verification_code")
    val verificationCode: String
)

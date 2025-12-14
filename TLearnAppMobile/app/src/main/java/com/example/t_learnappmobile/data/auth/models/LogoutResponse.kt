package com.example.t_learnappmobile.data.auth.models

import com.google.gson.annotations.SerializedName

data class LogoutResponse(
    @SerializedName("message")
    val message: String
)
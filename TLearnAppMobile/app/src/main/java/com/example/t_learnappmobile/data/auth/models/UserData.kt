package com.example.t_learnappmobile.data.auth.models

import com.google.gson.annotations.SerializedName

data class UserData(
    @SerializedName("id")
    val id: Int,
    @SerializedName("email")
    val email: String
)

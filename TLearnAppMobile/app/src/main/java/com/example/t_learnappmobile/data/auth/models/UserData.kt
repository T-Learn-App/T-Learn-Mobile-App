package com.example.t_learnappmobile.data.auth.models

import com.google.gson.annotations.SerializedName

data class UserData(
    @SerializedName("id")
    val id: Int,
    @SerializedName("email")
    val email: String,
    @SerializedName("firstName")
    val firstName: String? = null,
    @SerializedName("lastName")
    val lastName: String? = null,
    val fullName: String? = null  // для совместимости
)

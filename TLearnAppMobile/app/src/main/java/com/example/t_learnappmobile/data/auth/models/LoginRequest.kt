package com.example.t_learnappmobile.data.auth.models


import com.google.gson.annotations.SerializedName

data class LoginRequest(
    @SerializedName("login")
    val login: String,
    @SerializedName("password")
    val password: String
) {
    fun validate(): ValidationResult {
        return when {
            login.isBlank() -> ValidationResult.Error("Логин не может быть пустым")
            password.isBlank() -> ValidationResult.Error("Пароль не может быть пустым")
            else -> ValidationResult.Success
        }
    }
}
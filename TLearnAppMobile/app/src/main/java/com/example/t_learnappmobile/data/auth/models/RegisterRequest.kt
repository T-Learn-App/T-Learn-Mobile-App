package com.example.t_learnappmobile.data.auth.models


import com.google.gson.annotations.SerializedName

data class RegisterRequest(
    @SerializedName("email") val email: String,
    @SerializedName("password") val password: String,
    @SerializedName("firstName") val firstName: String,
    @SerializedName("lastName") val lastName: String
) {
    fun validate(): ValidationResult {
        return when {
            email.isBlank() -> ValidationResult.Error("Email не может быть пустым")
            !email.matches(Regex("^[A-Za-z0-9+_.-]+@(.+)$")) -> ValidationResult.Error("Некорректный email")
            firstName.isBlank() -> ValidationResult.Error("Имя не может быть пустым")
            lastName.isBlank() -> ValidationResult.Error("Фамилия не может быть пустой")
            password.isBlank() -> ValidationResult.Error("Пароль не может быть пустым")
            password.length < 8 -> ValidationResult.Error("Пароль минимум 8 символов")
            else -> ValidationResult.Success
        }
    }
}

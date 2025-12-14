package com.example.t_learnappmobile.data.auth.models


import com.google.gson.annotations.SerializedName

data class RegisterRequest(
    @SerializedName("login")
    val login: String,
    @SerializedName("email")
    val email: String,
    @SerializedName("password")
    val password: String
) {
    fun validate(): ValidationResult {
        return when {
            login.isBlank() -> ValidationResult.Error("Логин не может быть пустым")

            email.isBlank() -> ValidationResult.Error("Email не может быть пустым")
            !email.matches(Regex("^[A-Za-z0-9+_.-]+@(.+)$")) ->
                ValidationResult.Error("Некорректный email")

            password.isBlank() -> ValidationResult.Error("Пароль не может быть пустым")
            password.length < 8 -> ValidationResult.Error("Пароль минимум 8 символов")
            !password.any { it.isUpperCase() } ->
                ValidationResult.Error("Пароль должен содержать заглавные буквы")

            !password.any { it.isLowerCase() } ->
                ValidationResult.Error("Пароль должен содержать строчные буквы")

            !password.any { it.isDigit() } ->
                ValidationResult.Error("Пароль должен содержать цифры")

            !password.any { "!@#\$%^&*()_+=-[]{}|;:,.<>?".contains(it) } ->
                ValidationResult.Error("Пароль должен содержать спец символы")

            else -> ValidationResult.Success
        }
    }
}

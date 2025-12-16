package com.example.t_learnappmobile.data.auth.models

sealed class ValidationResult {
    data object Success : ValidationResult()
    data class Error(val message: String) : ValidationResult()
}

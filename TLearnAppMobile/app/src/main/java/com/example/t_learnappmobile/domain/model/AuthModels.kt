// domain/model/AuthModels.kt
package com.example.t_learnappmobile.domain.model

data class AuthResult(
    val userId: String,
    val email: String?
)

sealed class AuthError {
    data class ValidationError(val message: String) : AuthError()
    data class NetworkError(val message: String) : AuthError()
    data class FirebaseError(val message: String) : AuthError()
    object UnknownError : AuthError()
}
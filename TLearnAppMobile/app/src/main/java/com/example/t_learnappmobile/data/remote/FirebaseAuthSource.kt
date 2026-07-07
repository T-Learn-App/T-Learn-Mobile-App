package com.example.t_learnappmobile.data.remote

import android.util.Log
import com.example.t_learnappmobile.domain.model.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await

class FirebaseAuthSource {
    private val auth: FirebaseAuth = Firebase.auth
    private val TAG = "FirebaseAuth"

    suspend fun signIn(email: String, password: String): Result<AuthResult> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            val user = result.user ?: return Result.failure(Exception("Пользователь не найден"))
            Result.success(AuthResult(user.uid, user.email))
        } catch (e: Exception) {
            Log.e(TAG, "Sign in error", e)
            Result.failure(Exception(mapFirebaseError(e)))
        }
    }

    suspend fun signUp(email: String, password: String): Result<AuthResult> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val user = result.user ?: return Result.failure(Exception("Не удалось создать пользователя"))
            Result.success(AuthResult(user.uid, user.email))
        } catch (e: Exception) {
            Log.e(TAG, "Sign up error", e)
            Result.failure(Exception(mapFirebaseError(e)))
        }
    }

    fun signOut() {
        auth.signOut()
        Log.d(TAG, "User signed out")
    }

    fun getCurrentUserId(): String? {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            try {
                auth.currentUser?.reload()
            } catch (e: Exception) {
                Log.e(TAG, "Error reloading user", e)
            }
        }
        return userId
    }

    fun getCurrentUserEmail(): String? = auth.currentUser?.email

    fun isAuthenticated(): Boolean {
        val user = auth.currentUser
        if (user == null) return false

        return try {
            user.reload()
            true
        } catch (e: FirebaseAuthInvalidUserException) {
            Log.e(TAG, "User account is invalid, signing out")
            signOut()
            false
        } catch (e: Exception) {
            true
        }
    }

    private fun mapFirebaseError(e: Exception): String {
        return when (e) {
            is FirebaseAuthInvalidCredentialsException -> {
                when {
                    e.message?.contains("password", ignoreCase = true) == true -> "Неверный пароль"
                    e.message?.contains("email", ignoreCase = true) == true -> "Неверный формат email"
                    else -> "Неверные учетные данные"
                }
            }
            is FirebaseAuthInvalidUserException -> {
                when {
                    e.message?.contains("user not found", ignoreCase = true) == true -> "Пользователь не найден"
                    e.message?.contains("disabled", ignoreCase = true) == true -> "Аккаунт отключен"
                    else -> "Пользователь не найден"
                }
            }
            else -> {
                val message = e.message ?: return "Ошибка авторизации"
                val lowerMessage = message.lowercase()

                when {
                    lowerMessage.contains("email") && lowerMessage.contains("badly formatted") -> "Неверный формат электронной почты"
                    lowerMessage.contains("user not found") || lowerMessage.contains("no user record") -> "Пользователь не найден"
                    lowerMessage.contains("password is invalid") || lowerMessage.contains("wrong password") -> "Неверный пароль"
                    lowerMessage.contains("email already in use") -> "Этот email уже зарегистрирован"
                    lowerMessage.contains("password should be at least 6 characters") -> "Пароль должен содержать минимум 6 символов"
                    lowerMessage.contains("network error") -> "Ошибка сети. Проверьте подключение"
                    lowerMessage.contains("internal_error") -> "Внутренняя ошибка сервера. Попробуйте позже"
                    lowerMessage.contains("user_disabled") -> "Аккаунт отключен"
                    lowerMessage.contains("too many attempts") -> "Слишком много попыток. Попробуйте позже"
                    lowerMessage.contains("invalid email") -> "Неверный формат email"
                    lowerMessage.contains("weak password") -> "Слишком простой пароль"
                    lowerMessage.contains("operation not allowed") -> "Операция запрещена"
                    lowerMessage.contains("requires recent login") -> "Требуется повторный вход"
                    lowerMessage.contains("credential") && lowerMessage.contains("already in use") -> "Аккаунт уже существует"
                    lowerMessage.contains("network request failed") -> "Нет подключения к интернету"
                    lowerMessage.contains("timeout") -> "Превышено время ожидания"
                    lowerMessage.contains("invalid id token") -> "Сессия устарела. Войдите снова"
                    lowerMessage.contains("expired") -> "Сессия истекла. Войдите снова"
                    else -> "Ошибка: $message"
                }
            }
        }
    }
}
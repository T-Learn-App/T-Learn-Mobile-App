package com.example.t_learnappmobile.data.remote

import android.util.Log
import com.example.t_learnappmobile.domain.model.AuthResult
import com.google.firebase.auth.FirebaseAuth
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

    fun getCurrentUserId(): String? = auth.currentUser?.uid
    fun getCurrentUserEmail(): String? = auth.currentUser?.email
    fun isAuthenticated(): Boolean = auth.currentUser != null

    private fun mapFirebaseError(e: Exception): String {
        val message = e.message ?: return "Ошибка авторизации"
        return when {
            message.contains("The email address is badly formatted") -> "Неверный формат электронной почты"
            message.contains("There is no user record") -> "Пользователь не найден"
            message.contains("The password is invalid") -> "Введены некорректные данные"
            message.contains("The email address is already in use") -> "Электронная почта уже используется"
            message.contains("Password should be at least 6 characters") -> "Пароль должен содержать минимум 6 символов"
            message.contains("A network error") -> "Ошибка сети. Проверьте подключение"
            message.contains("INTERNAL_ERROR") -> "Внутренняя ошибка сервера. Попробуйте позже"
            message.contains("USER_DISABLED") -> "Аккаунт отключен"
            message.contains("TOO_MANY_ATTEMPTS_TRY_LATER") -> "Слишком много попыток. Попробуйте позже"
            else -> "Ошибка: $message"
        }
    }
}
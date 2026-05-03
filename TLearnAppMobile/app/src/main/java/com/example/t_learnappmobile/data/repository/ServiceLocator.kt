package com.example.t_learnappmobile.data.repository

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import com.example.t_learnappmobile.data.auth.*
import com.example.t_learnappmobile.data.firebase.FirebaseWordRepository
import com.example.t_learnappmobile.data.user.UserRepository
import com.example.t_learnappmobile.domain.repository.WordRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

@SuppressLint("StaticFieldLeak")
object ServiceLocator {
    lateinit var appContext: Context
    lateinit var wordRepository: WordRepository
    lateinit var userRepository: UserRepository

    // Firebase
    lateinit var firebaseAuth: FirebaseAuth
    lateinit var firestore: FirebaseFirestore
    lateinit var firebaseAuthManager: FirebaseAuthManager
    lateinit var authRepository: AuthRepository

    // API для карточек (бэкенд) - больше не используется, но оставим
    lateinit var wordApi: WordApi

    private val BACKEND_URL = "http://10.0.2.2:8080/"

    fun initContextAwareDependencies(appContext: Context) {
        this.appContext = appContext.applicationContext

        Log.d("ServiceLocator", "Initializing...")

        firebaseAuth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        firebaseAuthManager = FirebaseAuthManager()
        authRepository = AuthRepository(firebaseAuthManager)
        userRepository = UserRepository()

        // ВАЖНО: Создаем репозиторий слов на Firebase
        wordRepository = FirebaseWordRepository()

        Log.d("ServiceLocator", "Initialized successfully")
        Log.d("ServiceLocator", "WordRepository type: ${wordRepository::class.java.simpleName}")
    }
}
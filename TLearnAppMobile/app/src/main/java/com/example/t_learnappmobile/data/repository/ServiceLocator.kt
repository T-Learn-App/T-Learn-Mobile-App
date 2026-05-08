// Файл: data/repository/ServiceLocator.kt
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

@SuppressLint("StaticFieldLeak")
object ServiceLocator {
    lateinit var appContext: Context
    lateinit var wordRepository: WordRepository
    lateinit var userRepository: UserRepository

    lateinit var firebaseAuth: FirebaseAuth
    lateinit var firestore: FirebaseFirestore
    lateinit var firebaseAuthManager: FirebaseAuthManager
    lateinit var authRepository: AuthRepository

    fun initContextAwareDependencies(appContext: Context) {
        this.appContext = appContext.applicationContext

        Log.d("ServiceLocator", "Initializing...")

        firebaseAuth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        firebaseAuthManager = FirebaseAuthManager()
        authRepository = AuthRepository(firebaseAuthManager)
        userRepository = UserRepository()
        wordRepository = FirebaseWordRepository()

        Log.d("ServiceLocator", "Initialized successfully")
    }

    fun resetRepositories() {
        Log.d("ServiceLocator", "Resetting repositories...")
        (wordRepository as? FirebaseWordRepository)?.clearState()
        wordRepository = FirebaseWordRepository()
        userRepository = UserRepository()
        Log.d("ServiceLocator", "Repositories reset successfully")
    }
}
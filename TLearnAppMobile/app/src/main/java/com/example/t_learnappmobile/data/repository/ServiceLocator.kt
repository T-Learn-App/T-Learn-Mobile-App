// Файл: data/repository/ServiceLocator.kt
package com.example.t_learnappmobile.data.repository

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import com.example.t_learnappmobile.data.auth.*
import com.example.t_learnappmobile.data.sync.SyncManager
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
    lateinit var syncManager: SyncManager
    fun initContextAwareDependencies(appContext: Context) {
        this.appContext = appContext.applicationContext

        firebaseAuth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        firebaseAuthManager = FirebaseAuthManager()
        authRepository = AuthRepository(firebaseAuthManager)
        userRepository = UserRepository()

        // ✅ Создаем SyncManager и запускаем синхронизацию
        syncManager = SyncManager(appContext)
        syncManager.startPeriodicSync()
        wordRepository = HybridWordRepository(appContext, syncManager)
    }

    fun resetRepositories() {
        Log.d("ServiceLocator", "Resetting repositories...")
        (wordRepository as? HybridWordRepository)?.clearState()
        userRepository = UserRepository()
        Log.d("ServiceLocator", "Repositories reset successfully")
    }
}
package com.example.t_learnappmobile.data.repository

import android.content.Context
import com.example.t_learnappmobile.data.auth.*
import com.example.t_learnappmobile.data.dictionary.DictionaryManager
import com.example.t_learnappmobile.domain.repository.WordRepository
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockWebServer
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
object ServiceLocator {
    lateinit var tokenManager: TokenManager
    lateinit var authRepository: AuthRepository
    lateinit var dictionaryManager: DictionaryManager
    private lateinit var authApiService: AuthApiService

    private const val BACKEND_URL = "http://10.0.2.2:8080/"

    val api: WordApi by lazy {
        Retrofit.Builder()
            .baseUrl(BACKEND_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(WordApi::class.java)
    }

    val storage: WordsStorage by lazy { WordsStorage() }

    // ✅ ПУБЛИЧНЫЙ репозиторий для ViewModel'ов
    val wordRepository: WordRepository by lazy {
        WordRepositoryImpl(api, storage)
    }

    fun initContextAwareDependencies(context: Context) {
        tokenManager = TokenManager(context)
        dictionaryManager = DictionaryManager(context)
        authApiService = createAuthApiService(context, tokenManager)
        authRepository = AuthRepository(authApiService, tokenManager)
    }

    private fun createAuthApiService(context: Context, tokenManager: TokenManager): AuthApiService {
        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(AuthInterceptor(tokenManager) { BACKEND_URL })
            .addInterceptor(NetworkMonitorInterceptor(context))
            .build()

        return Retrofit.Builder()
            .baseUrl(BACKEND_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AuthApiService::class.java)
    }
}

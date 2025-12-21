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

    private var mockWebServer: MockWebServer? = null
    private var baseUrl: String? = null

    private val retrofitBuilder: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl("http://10.0.2.2:8080/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val api: WordApi by lazy { retrofitBuilder.create(WordApi::class.java) }
    val storage: WordsStorage by lazy { WordsStorage() }
    val wordRepository: WordRepository by lazy { WordRepositoryImpl(api, storage) }

    fun initContextAwareDependencies(context: Context) {
        tokenManager = TokenManager(context)
        dictionaryManager = DictionaryManager(context)
        authApiService = makeAuthApiService(context, tokenManager)
        authRepository = AuthRepository(authApiService, tokenManager)
    }

    private fun makeAuthApiService(context: Context, tokenManager: TokenManager): AuthApiService {
        initMockServerIfNeeded()
        val url = baseUrl ?: "https://localhost:8080/"

        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(AuthInterceptor(tokenManager) { baseUrl ?: "https://" })
            .addInterceptor(NetworkMonitorInterceptor(context))
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(url)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(GsonBuilder().create()))
            .build()

        return retrofit.create(AuthApiService::class.java)
    }

    private fun initMockServerIfNeeded() {
        if (mockWebServer == null && baseUrl == null) {
            Thread {
                try {
                    mockWebServer = MockWebServer()
                    mockWebServer!!.dispatcher = MockDispatcher()
                    mockWebServer!!.start()
                    baseUrl = mockWebServer!!.url("/api/").toString()
                } catch (e: Exception) {
                    e.printStackTrace()
                    baseUrl = "https://localhost:8080/api/"
                }
            }.apply {
                start()
                join()
            }
        }
    }
}

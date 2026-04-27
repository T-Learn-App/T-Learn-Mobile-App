package com.example.t_learnappmobile.data.repository

import android.annotation.SuppressLint
import android.content.Context
import com.example.t_learnappmobile.data.auth.*
import com.example.t_learnappmobile.data.game.GameApiService
import com.example.t_learnappmobile.data.leaderboard.LeaderboardApi
import com.example.t_learnappmobile.data.leaderboard.LeaderboardManager
import com.example.t_learnappmobile.data.user.UserApiService
import com.example.t_learnappmobile.domain.repository.WordRepository
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

@SuppressLint("StaticFieldLeak")
object ServiceLocator {
    lateinit var appContext: Context
    lateinit var tokenManager: TokenManager
    lateinit var authRepository: AuthRepository
    lateinit var userApiService: UserApiService
    lateinit var gameApiService: GameApiService
    lateinit var leaderboardManager: LeaderboardManager
    lateinit var leaderboardApi: LeaderboardApi

    val wordRepository: WordRepository by lazy {
        requireInitialized()
        WordRepositoryImpl(api, storage)
    }

    private val BACKEND_URL = com.example.t_learnappmobile.BuildConfig.BASE_URL

    val api: WordApi by lazy {
        requireInitialized()
        Retrofit.Builder()
            .baseUrl(BACKEND_URL)
            .client(createApiClient())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(WordApi::class.java)
    }

    val storage: WordsStorage by lazy { WordsStorage() }
    private lateinit var authApiService: AuthApiService

    fun initContextAwareDependencies(appContext: Context) {
        require(!this::appContext.isInitialized) {
            "ServiceLocator уже инициализирован"
        }
        this.appContext = appContext.applicationContext

        tokenManager = TokenManager(appContext)

        leaderboardApi = createLeaderboardApiService()
        leaderboardManager = LeaderboardManager(leaderboardApi)

        authApiService = createAuthApiService()
        authRepository = AuthRepository(authApiService, tokenManager)
        gameApiService = createGameApiService()
        userApiService = createUserApiService()
    }

    private fun createLeaderboardApiService(): LeaderboardApi {
        val client = createRealClient()
        return Retrofit.Builder()
            .baseUrl(BACKEND_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(LeaderboardApi::class.java)
    }

    private fun createAuthApiService(): AuthApiService {
        val client = OkHttpClient.Builder()
            .addInterceptor(NetworkMonitorInterceptor(appContext))
            .build()
        return Retrofit.Builder()
            .baseUrl(BACKEND_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AuthApiService::class.java)
    }

    private fun createGameApiService(): GameApiService {
        val client = createRealClient()
        return Retrofit.Builder()
            .baseUrl(BACKEND_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(GameApiService::class.java)
    }

    private fun createUserApiService(): UserApiService {
        val client = createRealClient()
        return Retrofit.Builder()
            .baseUrl(BACKEND_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(UserApiService::class.java)
    }

    private fun createRealClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .addInterceptor(AuthInterceptor(tokenManager) { BACKEND_URL })
            .addInterceptor(NetworkMonitorInterceptor(appContext))
            .build()
    }

    private fun createApiClient(): OkHttpClient = createRealClient()

    private fun requireInitialized(): Unit =
        if (!this::appContext.isInitialized) {
            error("ServiceLocator.initContextAwareDependencies не вызван")
        } else Unit
}
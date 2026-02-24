package com.example.t_learnappmobile.data.repository

import android.annotation.SuppressLint
import android.content.Context
import androidx.room.Room
import com.example.t_learnappmobile.data.auth.*
import com.example.t_learnappmobile.data.dictionary.DictionaryManager
import com.example.t_learnappmobile.data.game.GameDatabase
import com.example.t_learnappmobile.data.game.GameResultDao
import com.example.t_learnappmobile.data.leaderboard.LeaderboardManager
import com.example.t_learnappmobile.data.statistics.DailyStatsDao
import com.example.t_learnappmobile.data.statistics.StatsDatabase
import com.example.t_learnappmobile.domain.repository.WordRepository
import kotlin.require
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/*
* ServiceLocator - это singleton, реализующий паттерн Service Locator.
* Объект, который хранит зависимости и позволяет их извлекать при
* необходимости.
* */
@SuppressLint("StaticFieldLeak")
object ServiceLocator {
    // Application Context
    @SuppressLint("StaticFieldLeak")
    lateinit var appContext: Context

    // Публичные для совместимости с проектом (ApplicationContext внутри!)
    @SuppressLint("StaticFieldLeak")
    lateinit var tokenManager: TokenManager

    @SuppressLint("StaticFieldLeak")
    lateinit var dictionaryManager: DictionaryManager

    // Репозитории
    @SuppressLint("StaticFieldLeak")
    lateinit var authRepository: AuthRepository

    val wordRepository: WordRepository by lazy {
        requireInitialized()
        WordRepositoryImpl(api, storage)
    }

    // Базы данных
    @SuppressLint("StaticFieldLeak")
    lateinit var statsDatabase: StatsDatabase

    lateinit var dailyStatsDao: DailyStatsDao
    lateinit var gameDatabase: GameDatabase
    lateinit var gameResultDao: GameResultDao

    // Менеджеры без контекста
    lateinit var leaderboardManager: LeaderboardManager

    // API (lazy)
    private const val BACKEND_URL = "http://10.0.2.2:8080/"

    val api: WordApi by lazy {
        requireInitialized()
        Retrofit.Builder()
            .baseUrl(BACKEND_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(WordApi::class.java)
    }
    val storage: WordsStorage by lazy { WordsStorage() }

    // Приватные API сервисы
    private lateinit var authApiService: AuthApiService

    // Инициализация (вызывается только в Application.onCreate()
    fun initContextAwareDependencies(appContext: Context) {
        require(!this::appContext.isInitialized) {
            "ServiceLocator уже инициализирован"
        }
        this.appContext = appContext.applicationContext

        // Stats Database
        statsDatabase = Room.databaseBuilder(
            appContext,
            StatsDatabase::class.java,
            "stats_database"
        )
            .fallbackToDestructiveMigrationOnDowngrade()
            .build()
        dailyStatsDao = statsDatabase.dailyStatsDao()

        // Game Database
        gameDatabase = Room.databaseBuilder(
            appContext,
            GameDatabase::class.java,
            "game_database"
        )
            .fallbackToDestructiveMigration() // только для разработки
            .build()
        gameResultDao = gameDatabase.gameResultDao()

        // Менеджеры с ApplicationContext
        tokenManager = TokenManager(appContext)
        dictionaryManager = DictionaryManager(appContext)
        leaderboardManager = LeaderboardManager()

        // Auth API
        authApiService = createAuthApiService()
        authRepository = AuthRepository(authApiService, tokenManager)
    }

    private fun createAuthApiService(): AuthApiService {
        requireInitialized()

        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(AuthInterceptor(tokenManager) { BACKEND_URL })
            .addInterceptor(NetworkMonitorInterceptor(appContext))
            .build()

        return Retrofit.Builder()
            .baseUrl(BACKEND_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AuthApiService::class.java)
    }

    private fun requireInitialized(): Unit =
        if (!this::appContext.isInitialized) {
            error("ServiceLocator.initContextAwareDependencies(appContext) не вызван")
        } else Unit
}

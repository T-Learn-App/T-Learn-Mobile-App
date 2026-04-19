package com.example.t_learnappmobile.data.repository

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import androidx.room.Room
import com.example.t_learnappmobile.data.auth.*
import com.example.t_learnappmobile.data.auth.models.LoginRequest
import com.example.t_learnappmobile.data.dictionary.DictionaryManager
import com.example.t_learnappmobile.data.game.GameApiService
import com.example.t_learnappmobile.data.game.GameDatabase
import com.example.t_learnappmobile.data.game.GameResultDao
import com.example.t_learnappmobile.data.leaderboard.LeaderboardManager
import com.example.t_learnappmobile.data.statistics.DailyStatsDao
import com.example.t_learnappmobile.data.statistics.StatsDatabase
import com.example.t_learnappmobile.data.user.UserApiService
import com.example.t_learnappmobile.data.user.UserResponse
import com.example.t_learnappmobile.domain.repository.WordRepository
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withTimeout
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

@SuppressLint("StaticFieldLeak")
object ServiceLocator {
    lateinit var appContext: Context
    lateinit var tokenManager: TokenManager
    lateinit var dictionaryManager: DictionaryManager
    lateinit var authRepository: AuthRepository

    val wordRepository: WordRepository by lazy {
        requireInitialized()
        WordRepositoryImpl(api, storage)
    }

    lateinit var statsDatabase: StatsDatabase
    lateinit var dailyStatsDao: DailyStatsDao
    lateinit var gameDatabase: GameDatabase
    lateinit var gameResultDao: GameResultDao
    lateinit var leaderboardManager: LeaderboardManager
    lateinit var gameApiService: GameApiService
    lateinit var userApiService: UserApiService

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



        initDatabases()
        tokenManager = TokenManager(appContext)
        dictionaryManager = DictionaryManager(appContext)
        leaderboardManager = LeaderboardManager()


        authApiService = createAuthApiService()
        authRepository = AuthRepository(authApiService, tokenManager)
        gameApiService = createGameApiService()
        userApiService = createUserApiService()


    }

    private fun initDatabases() {
        statsDatabase = Room.databaseBuilder(
            appContext, StatsDatabase::class.java, "stats_database"
        ).fallbackToDestructiveMigrationOnDowngrade().build()
        dailyStatsDao = statsDatabase.dailyStatsDao()

        gameDatabase = Room.databaseBuilder(
            appContext, GameDatabase::class.java, "game_database"
        ).fallbackToDestructiveMigration().build()
        gameResultDao = gameDatabase.gameResultDao()
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
        val baseUrl = BACKEND_URL


        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(GameApiService::class.java)
    }

    private fun createUserApiService(): UserApiService {

        val client = createRealClient()
        val baseUrl = BACKEND_URL


        return Retrofit.Builder()
            .baseUrl(baseUrl)
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
            .addInterceptor { chain ->
                val request = chain.request()


                val response = chain.proceed(request)



                response
            }
            .addInterceptor(AuthInterceptor(tokenManager) { BACKEND_URL } )
            .addInterceptor(NetworkMonitorInterceptor(appContext))
            .build()
    }



    private fun createApiClient(): OkHttpClient = createRealClient()






    private fun requireInitialized(): Unit =
        if (!this::appContext.isInitialized) {
            error("ServiceLocator.initContextAwareDependencies не вызван")
        } else Unit



}

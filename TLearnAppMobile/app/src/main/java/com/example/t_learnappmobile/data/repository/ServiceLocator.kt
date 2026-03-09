package com.example.t_learnappmobile.data.repository

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import androidx.room.Room
import com.example.t_learnappmobile.data.auth.*
import com.example.t_learnappmobile.data.auth.models.LoginRequest
import com.example.t_learnappmobile.data.dictionary.DictionaryManager
import com.example.t_learnappmobile.data.game.GameDatabase
import com.example.t_learnappmobile.data.game.GameResultDao
import com.example.t_learnappmobile.data.leaderboard.LeaderboardManager
import com.example.t_learnappmobile.data.statistics.DailyStatsDao
import com.example.t_learnappmobile.data.statistics.StatsDatabase
import com.example.t_learnappmobile.domain.repository.WordRepository
import kotlinx.coroutines.withTimeout
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
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

    private const val BACKEND_URL = "https://856eed6b-a8d1-44b0-8253-ef83deebf67d.mock.pstmn.io/"
    private const val MOCK_SERVER_PORT = 8081
//    private var mockServerUrl: String = "https://856eed6b-a8d1-44b0-8253-ef83deebf67d.mock.pstmn.io/"
//    private var useMockServer = false
//    private var mockWebServer: MockWebServer? = null

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

        Log.i("ServiceLocator", "🚀 Graceful Fallback инициализация...")

        initDatabases()
        tokenManager = TokenManager(appContext)
        dictionaryManager = DictionaryManager(appContext)
        leaderboardManager = LeaderboardManager()


        authApiService = createAuthApiService()
        authRepository = AuthRepository(authApiService, tokenManager)


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
        Log.d("🔐 ServiceLocator", "🔨 Creating AuthApiService")
        val client = createRealClient()
        val baseUrl = BACKEND_URL
        Log.d("🔐 ServiceLocator", "🌐 Auth baseUrl: $baseUrl")

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AuthApiService::class.java)
    }


    private fun createRealClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val request = chain.request()
                Log.d("RealAPI", "➡️ ${request.method} ${request.url}")

                val response = chain.proceed(request)

                // ✅ УДАЛИЛ response.body?.string()!
                Log.d("RealAPI", "⬅️ ${response.code} | Body: [не читаем body]")

                response  // ← Body теперь доступен для Gson!
            }
            .addInterceptor(AuthInterceptor(tokenManager) { BACKEND_URL })
            .addInterceptor(NetworkMonitorInterceptor(appContext))
            .build()
    }



    private fun createApiClient(): OkHttpClient = createRealClient()

    suspend fun testServerConnectivity(): Boolean {
        return try {
            withTimeout(4000L) {
                val testClient = OkHttpClient.Builder().connectTimeout(4, TimeUnit.SECONDS).build()
                val testRetrofit = Retrofit.Builder()
                    .baseUrl(BACKEND_URL)
                    .client(testClient)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()
                val testApi = testRetrofit.create(AuthApiService::class.java)
                testApi.login(LoginRequest("test@test.com", "test")).isSuccessful
            }
        } catch (e: Exception) {
            Log.w("ServiceLocator", "🔍 Сервер недоступен: ${e.message}")
            false
        }
    }




    private fun requireInitialized(): Unit =
        if (!this::appContext.isInitialized) {
            error("ServiceLocator.initContextAwareDependencies не вызван")
        } else Unit
}

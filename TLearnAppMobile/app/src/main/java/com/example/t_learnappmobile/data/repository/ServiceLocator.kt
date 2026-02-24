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

    private const val BACKEND_URL = "http://10.0.2.2:8080/"
    private const val MOCK_SERVER_PORT = 8081
    private var mockServerUrl: String = "http://localhost:$MOCK_SERVER_PORT/"
    private var useMockServer = true
    private var mockWebServer: MockWebServer? = null

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

        // ✅ Встроенный Mock Dispatcher (НЕ внешний MockDispatcher класс)
        startMockWebServer()
        authApiService = createAuthApiService()
        authRepository = AuthRepository(authApiService, tokenManager)

        Log.i("ServiceLocator", "✅ Инициализация завершена (Mock=$useMockServer)")
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

    // 🔥 Встроенный MockWebServer Dispatcher
    private fun startMockWebServer() {
        try {
            mockWebServer = MockWebServer()

            // ✅ ПРАВИЛЬНЫЙ синтаксис для Kotlin
            mockWebServer!!.dispatcher = object : okhttp3.mockwebserver.Dispatcher() {
                override fun dispatch(request: RecordedRequest): MockResponse {
                    return when {
                        request.path?.contains("auth/login") == true -> handleAuthLogin(request)
                        request.path?.contains("auth/token/refresh") == true -> handleAuthRefresh()
                        request.path?.contains("auth/check-email") == true -> handleCheckEmail(request)
                        else -> MockResponse()
                            .setResponseCode(404)
                            .setBody("""{"error":"Mock: Not found"}""")
                    }
                }

                private fun handleAuthLogin(request: RecordedRequest): MockResponse {
                    return try {
                        val body = request.body.readUtf8()
                        val json = org.json.JSONObject(body)
                        val email = json.optString("email", "test@test.com")

                        MockResponse()
                            .setResponseCode(200)
                            .addHeader("Content-Type", "application/json")
                            .setBody("""
                                {
                                    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VySWQiOiIxIiwiZW1haWwiOi"${email}","exp":9999999999},"mock-jwt",
                                    "refreshToken": "mock-refresh-${System.currentTimeMillis()}"
                                }
                            """.trimIndent())
                    } catch (e: Exception) {
                        MockResponse().setResponseCode(400).setBody("""{"error":"Invalid request"}""")
                    }
                }

                private fun handleAuthRefresh(): MockResponse {
                    return MockResponse()
                        .setResponseCode(200)
                        .addHeader("Content-Type", "application/json")
                        .setBody("""
                            {
                                "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VySWQiOiIxIiwiZXhwIjo5OTk5OTk5OTk5fQ.mock-new",
                                "refreshToken": "mock-refresh-new-${System.currentTimeMillis()}"
                            }
                        """.trimIndent())
                }

                private fun handleCheckEmail(request: RecordedRequest): MockResponse {
                    return try {
                        val body = request.body.readUtf8()
                        val json = org.json.JSONObject(body)
                        val email = json.optString("email", "")
                        val exists = email == "test@test.com" // Mock логика

                        MockResponse()
                            .setResponseCode(200)
                            .setBody("""{"exists":$exists}""")
                    } catch (e: Exception) {
                        MockResponse().setResponseCode(400).setBody("""{"error":"Invalid request"}""")
                    }
                }
            }

            mockWebServer!!.start(MOCK_SERVER_PORT)
            useMockServer = true
            Log.i("ServiceLocator", "✅ MockWebServer запущен на $MOCK_SERVER_PORT")
        } catch (e: Exception) {
            Log.e("ServiceLocator", "❌ MockWebServer ошибка: ${e.message}")
            useMockServer = false
        }
    }

    private fun createAuthApiService(): AuthApiService {
        requireInitialized()
        val client = if (useMockServer) createMockClient() else createRealClient()
        val baseUrl = if (useMockServer) mockServerUrl else BACKEND_URL

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AuthApiService::class.java)
    }

    private fun createMockClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(3, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .writeTimeout(5, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                Log.d("MockAPI", "📡 ${chain.request().url}")
                chain.proceed(chain.request())
            }
            .addInterceptor(AuthInterceptor(tokenManager) { mockServerUrl })
            .build()
    }

    private fun createRealClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                Log.d("RealAPI", "🌐 ${chain.request().url}")
                chain.proceed(chain.request())
            }
            .addInterceptor(AuthInterceptor(tokenManager) { BACKEND_URL })
            .addInterceptor(NetworkMonitorInterceptor(appContext))
            .build()
    }

    private fun createApiClient(): OkHttpClient = if (useMockServer) createMockClient() else createRealClient()

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

    suspend fun switchToRealServer() {
        if (!useMockServer || mockWebServer == null) return

        if (testServerConnectivity()) {
            useMockServer = false
            Log.i("ServiceLocator", "✅ Переключение на реальный сервер")
            authApiService = createAuthApiService()
            authRepository = AuthRepository(authApiService, tokenManager)
        }
    }

    fun stopMockServer() {
        try {
            mockWebServer?.shutdown()
            mockWebServer = null
            useMockServer = false
            Log.i("ServiceLocator", "🛑 MockWebServer остановлен")
        } catch (e: Exception) {
            Log.e("ServiceLocator", "Ошибка остановки: ${e.message}")
        }
    }

    private fun requireInitialized(): Unit =
        if (!this::appContext.isInitialized) {
            error("ServiceLocator.initContextAwareDependencies не вызван")
        } else Unit
}

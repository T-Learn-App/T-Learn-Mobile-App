// di/AppModule.kt
package com.example.t_learnappmobile.di

import android.content.Context
import com.example.t_learnappmobile.data.local.AppDatabase
import com.example.t_learnappmobile.data.local.SettingsLocalSource
import com.example.t_learnappmobile.data.local.WordLocalSource
import com.example.t_learnappmobile.data.remote.FirebaseAuthSource
import com.example.t_learnappmobile.data.remote.FirebaseFirestoreSource
import com.example.t_learnappmobile.data.remote.FirebaseGameSource
import com.example.t_learnappmobile.data.repository.*
import com.example.t_learnappmobile.data.sync.SyncManager
import com.example.t_learnappmobile.domain.repository.AuthRepository
import com.example.t_learnappmobile.domain.repository.GameRepository
import com.example.t_learnappmobile.domain.repository.UserRepository
import com.example.t_learnappmobile.domain.repository.WordRepository
import com.example.t_learnappmobile.domain.usecase.auth.LoginUseCase
import com.example.t_learnappmobile.domain.usecase.auth.RegisterUseCase
import com.example.t_learnappmobile.domain.usecase.game.*
import com.example.t_learnappmobile.domain.usecase.settings.SettingsUseCase
import com.example.t_learnappmobile.domain.usecase.user.ResetUserDataUseCase
import com.example.t_learnappmobile.domain.usecase.user.UpdateProfileUseCase
import com.example.t_learnappmobile.domain.usecase.words.*

class AppModule(private val context: Context) {

    // Data Sources
    private val database by lazy { AppDatabase.getInstance(context) }
    val wordLocalSource by lazy { WordLocalSource(database.wordDao()) }
    val settingsLocalSource by lazy { SettingsLocalSource(context) }
    val firebaseAuthSource by lazy { FirebaseAuthSource() }
    val firebaseFirestoreSource by lazy { FirebaseFirestoreSource() }
    val firebaseGameSource by lazy {
        FirebaseGameSource(
            firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance(),
            context = context
        )
    }

    // Sync - теперь принимает 3 параметра
    val syncManager by lazy {
        SyncManager(
            localSource = wordLocalSource,
            remoteSource = firebaseFirestoreSource,
            authSource = firebaseAuthSource
        )
    }

    // Repositories
    val authRepository: AuthRepository by lazy {
        AuthRepositoryImpl(firebaseAuthSource, firebaseFirestoreSource)
    }
    val wordRepository: WordRepository by lazy {
        WordRepositoryImpl(wordLocalSource, firebaseFirestoreSource)
    }
    val userRepository: UserRepository by lazy {
        UserRepositoryImpl(firebaseFirestoreSource, firebaseAuthSource)
    }
    val gameRepository: GameRepository by lazy {
        GameRepositoryImpl(firebaseGameSource, firebaseAuthSource, userRepository)
    }

    // Use Cases
    val loginUseCase by lazy { LoginUseCase(authRepository) }
    val registerUseCase by lazy { RegisterUseCase(authRepository, userRepository) }
    val loadWordsUseCase by lazy { LoadWordsUseCase(wordRepository) }
    val processAnswerUseCase by lazy { ProcessAnswerUseCase(wordRepository) }
    val getDictionariesUseCase by lazy { GetDictionariesUseCase(wordRepository) }
    val getWordStatsUseCase by lazy { GetWordStatsUseCase(wordRepository) }
    val loadGameWordsUseCase by lazy { LoadGameWordsUseCase(gameRepository) }
    val saveGameResultUseCase by lazy { SaveGameResultUseCase(gameRepository, userRepository) }
    val getWeeklyStatsUseCase by lazy { GetWeeklyStatsUseCase(gameRepository) }
    val getLeaderboardUseCase by lazy { GetLeaderboardUseCase(gameRepository) }
    val updateProfileUseCase by lazy { UpdateProfileUseCase(userRepository) }
    val resetUserDataUseCase by lazy { ResetUserDataUseCase(userRepository, wordRepository, syncManager) }
    val settingsUseCase by lazy { SettingsUseCase(settingsLocalSource) }
}
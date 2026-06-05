package com.example.t_learnappmobile.presentation.navigation

import android.util.Log
import androidx.compose.runtime.*
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.t_learnappmobile.di.AppModule
import com.example.t_learnappmobile.presentation.auth.AuthViewModel
import com.example.t_learnappmobile.presentation.auth.LoginScreen
import com.example.t_learnappmobile.presentation.auth.RegistrationScreen
import com.example.t_learnappmobile.presentation.cards.CardsScreen
import com.example.t_learnappmobile.presentation.cards.CardsViewModel
import com.example.t_learnappmobile.presentation.components.NotificationManager
import com.example.t_learnappmobile.presentation.game.GameScreen
import com.example.t_learnappmobile.presentation.game.GameViewModel
import com.example.t_learnappmobile.presentation.settings.SettingsScreen
import com.example.t_learnappmobile.presentation.settings.SettingsViewModel
import com.example.t_learnappmobile.presentation.statistics.StatisticsScreen
import com.example.t_learnappmobile.presentation.statistics.StatisticsViewModel

private const val TAG = "NavGraph"

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Registration : Screen("registration")
    object Cards : Screen("cards")
    object Game : Screen("game")
    object Settings : Screen("settings")
    object Statistics : Screen("statistics")
}

@Composable
fun NavGraph(
    notificationManager: NotificationManager,
    onThemeChanged: (Boolean) -> Unit = {},
    appModule: AppModule,
    isConnected: Boolean
) {
    val navController = rememberNavController()

    Log.d(TAG, "NavGraph recompose")

    val authViewModel = remember {
        AuthViewModel(
            loginUseCase = appModule.loginUseCase,
            registerUseCase = appModule.registerUseCase,
            authRepository = appModule.authRepository,
            wordRepository = appModule.wordRepository,
            gameLocalSource = appModule.gameLocalSource,
            settingsLocalSource = appModule.settingsLocalSource
        )
    }
    val cardsViewModel = remember {
        CardsViewModel(
            authRepository = appModule.authRepository,
            loadWordsUseCase = appModule.loadWordsUseCase,
            processAnswerUseCase = appModule.processAnswerUseCase,
            getDictionariesUseCase = appModule.getDictionariesUseCase,
            settingsUseCase = appModule.settingsUseCase,
            syncManager = appModule.syncManager
        )
    }


    val gameViewModel = remember {
        GameViewModel(
            loadGameWordsUseCase = appModule.loadGameWordsUseCase,
            saveGameResultUseCase = appModule.saveGameResultUseCase,
            settingsUseCase = appModule.settingsUseCase,
            wordRepository = appModule.wordRepository
        )
    }

    val settingsViewModel = remember {
        SettingsViewModel(
            getDictionariesUseCase = appModule.getDictionariesUseCase,
            updateProfileUseCase = appModule.updateProfileUseCase,
            settingsUseCase = appModule.settingsUseCase,
            authRepository = appModule.authRepository,
            userRepository = appModule.userRepository,
            wordRepository = appModule.wordRepository
        )
    }

    val statisticsViewModel = remember {
        StatisticsViewModel(
            getWordStatsUseCase = appModule.getWordStatsUseCase,
            getWeeklyStatsUseCase = appModule.getWeeklyStatsUseCase,
            getLeaderboardUseCase = appModule.getLeaderboardUseCase,
            authRepository = appModule.authRepository,
            userRepository = appModule.userRepository,
            settingsUseCase = appModule.settingsUseCase,
            syncManager = appModule.syncManager,
            gameLocalSource = appModule.gameLocalSource
        )
    }

    LaunchedEffect(Unit) {
        Log.d(TAG, "NavGraph LaunchedEffect - checkAuthState")
        authViewModel.checkAuthState()
    }

    val startDestination = remember {
        val dest = if (appModule.authRepository.isAuthenticated()) {
            Screen.Cards.route
        } else {
            Screen.Login.route
        }
        Log.d(TAG, "startDestination = $dest")
        dest
    }

    NavHost(navController = navController, startDestination = startDestination) {
        composable(Screen.Login.route) {
            LoginScreen(
                authViewModel = authViewModel,
                notificationManager = notificationManager,
                onLoginSuccess = {
                    Log.d(TAG, "Login success, navigating to Cards")
                    statisticsViewModel.onUserChanged()
                    cardsViewModel.resetAndReload()
                    navController.navigate(Screen.Cards.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onNavigateToRegistration = {
                    Log.d(TAG, "Navigate to Registration")
                    navController.navigate(Screen.Registration.route)
                }
            )
        }

        composable(Screen.Registration.route) {
            RegistrationScreen(
                authViewModel = authViewModel,
                notificationManager = notificationManager,
                onRegisterSuccess = {
                    Log.d(TAG, "Register success, navigating to Cards")
                    cardsViewModel.resetAndReload()
                    navController.navigate(Screen.Cards.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onNavigateToLogin = {
                    Log.d(TAG, "Navigate to Login")
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Registration.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Cards.route) {
            CardsScreen(
                viewModel = cardsViewModel,
                onNavigateToGame = {
                    Log.d(TAG, "Navigate to Game from Cards")
                    if (isConnected) {
                        navController.navigate(Screen.Game.route)
                    } else {
                        notificationManager.showError("Для игры требуется интернет-соединение")
                    }
                },
                onNavigateToSettings = {
                    Log.d(TAG, "Navigate to Settings from Cards")
                    navController.navigate(Screen.Settings.route)
                },
                onNavigateToStatistics = {
                    Log.d(TAG, "Navigate to Statistics from Cards")
                    if (isConnected) {
                        navController.navigate(Screen.Statistics.route)
                    } else {
                        notificationManager.showError("Статистика требует интернет-соединение")
                    }
                },
                onLogout = {
                    Log.d(TAG, "Logout from Cards")
                    authViewModel.logoutAndClearData()
                    statisticsViewModel.onUserChanged()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Cards.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Game.route) {
            GameScreen(
                viewModel = gameViewModel,
                onGameFinished = {
                    Log.d(TAG, "Game finished, popBackStack")
                    navController.popBackStack()
                },
                isConnected = isConnected
            )
        }


        composable(Screen.Settings.route) {
            LaunchedEffect(Unit) {
                settingsViewModel.refreshData()
            }

            SettingsScreen(
                viewModel = settingsViewModel,
                notificationManager = notificationManager,
                onDictionaryChanged = { dictionaryId ->
                    Log.d(TAG, "Dictionary changed in settings: $dictionaryId")
                    cardsViewModel.selectDictionary(dictionaryId)
                },
                onClose = {
                    Log.d(TAG, "Settings close clicked, popBackStack")
                    navController.popBackStack()
                },
                onLogout = {
                    Log.d(TAG, "Logout from Settings")
                    authViewModel.logoutAndClearData()
                    statisticsViewModel.onUserChanged()
                    cardsViewModel.resetAndReload()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onThemeChanged = onThemeChanged,
                isConnected = isConnected,
                onDataReset = {
                    Log.d(TAG, "Data reset from Settings")
                    statisticsViewModel.forceRefresh()
                }
            )
        }

        composable(Screen.Statistics.route) {
            StatisticsScreen(
                viewModel = statisticsViewModel,
                onClose = {
                    Log.d(TAG, "Statistics close clicked, popBackStack")
                    navController.popBackStack()
                },
                isConnected = isConnected
            )
        }
    }
}
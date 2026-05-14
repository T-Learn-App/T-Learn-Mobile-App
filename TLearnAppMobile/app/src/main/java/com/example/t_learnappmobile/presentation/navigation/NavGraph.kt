// presentation/navigation/NavGraph.kt
package com.example.t_learnappmobile.presentation.navigation

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
    appModule: AppModule
) {
    val navController = rememberNavController()

    val authViewModel = remember {
        AuthViewModel(
            loginUseCase = appModule.loginUseCase,
            registerUseCase = appModule.registerUseCase,
            authRepository = appModule.authRepository
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
            settingsUseCase = appModule.settingsUseCase
        )
    }

    val settingsViewModel = remember {
        SettingsViewModel(
            getDictionariesUseCase = appModule.getDictionariesUseCase,
            updateProfileUseCase = appModule.updateProfileUseCase,
            resetUserDataUseCase = appModule.resetUserDataUseCase,
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
            settingsUseCase = appModule.settingsUseCase
        )
    }

    val authState by authViewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        authViewModel.checkAuthState()
    }

    val startDestination = remember {
        if (appModule.authRepository.isAuthenticated()) {
            Screen.Cards.route
        } else {
            Screen.Login.route
        }
    }

    NavHost(navController = navController, startDestination = startDestination) {

        composable(Screen.Login.route) {
            LoginScreen(
                authViewModel = authViewModel,
                notificationManager = notificationManager,
                onLoginSuccess = {
                    cardsViewModel.resetAndReload()
                    navController.navigate(Screen.Cards.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onNavigateToRegistration = {
                    navController.navigate(Screen.Registration.route)
                }
            )
        }

        composable(Screen.Registration.route) {
            RegistrationScreen(
                authViewModel = authViewModel,
                notificationManager = notificationManager,
                onRegisterSuccess = {
                    cardsViewModel.resetAndReload()
                    navController.navigate(Screen.Cards.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onNavigateToLogin = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Registration.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Cards.route) {
            CardsScreen(
                viewModel = cardsViewModel,
                notificationManager = notificationManager,
                onNavigateToGame = {
                    navController.navigate(Screen.Game.route)
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                },
                onNavigateToStatistics = {
                    navController.navigate(Screen.Statistics.route)
                },
                onLogout = {
                    authViewModel.logout()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Cards.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Game.route) {
            GameScreen(
                viewModel = gameViewModel,
                notificationManager = notificationManager,
                onGameFinished = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.Settings.route) {
            LaunchedEffect(Unit) {
                settingsViewModel.refreshUserData()
            }
            SettingsScreen(
                viewModel = settingsViewModel,
                notificationManager = notificationManager,
                onDictionaryChanged = { dictionaryId ->
                    cardsViewModel.selectDictionary(dictionaryId)
                },
                onClose = {
                    navController.popBackStack()
                },
                onLogout = {
                    authViewModel.logout()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onThemeChanged = onThemeChanged
            )
        }

        composable(Screen.Statistics.route) {
            StatisticsScreen(
                viewModel = statisticsViewModel,
                onClose = {
                    navController.popBackStack()
                }
            )
        }
    }
}
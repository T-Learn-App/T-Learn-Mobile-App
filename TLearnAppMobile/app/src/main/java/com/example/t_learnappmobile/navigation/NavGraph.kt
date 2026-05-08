package com.example.t_learnappmobile.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.t_learnappmobile.data.repository.ServiceLocator
import com.example.t_learnappmobile.presentation.auth.LoginScreen
import com.example.t_learnappmobile.presentation.auth.RegistrationScreen
import com.example.t_learnappmobile.presentation.auth.AuthViewModel
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
    authViewModel: AuthViewModel = viewModel(),
    cardsViewModel: CardsViewModel = viewModel(),
    gameViewModel: GameViewModel = viewModel(),
    settingsViewModel: SettingsViewModel = viewModel(),
    statisticsViewModel: StatisticsViewModel = viewModel()
) {
    val navController = rememberNavController()
    val authState by authViewModel.authState.collectAsState()

    LaunchedEffect(Unit) {
        authViewModel.checkAuthState()
    }

    val startDestination = if (authState.isSuccess) {
        Screen.Cards.route
    } else {
        Screen.Login.route
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Login.route) {
            LaunchedEffect(Unit) {
                ServiceLocator.resetRepositories()
                cardsViewModel.resetAndReload()
            }
            LoginScreen(
                authViewModel = authViewModel,
                notificationManager = notificationManager,
                onLoginSuccess = {
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
                    ServiceLocator.resetRepositories()
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

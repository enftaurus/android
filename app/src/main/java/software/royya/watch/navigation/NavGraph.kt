package software.royya.watch.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import software.royya.watch.features.alertdetails.AlertDetailsScreen
import software.royya.watch.features.alertdetails.AlertDetailsViewModel
import software.royya.watch.features.alerts.AlertsScreen
import software.royya.watch.features.alerts.AlertsViewModel
import software.royya.watch.features.home.HomeScreen
import software.royya.watch.features.home.HomeViewModel
import software.royya.watch.features.onboarding.OnboardingScreen
import software.royya.watch.features.onboarding.OnboardingViewModel
import software.royya.watch.features.ponddetails.PondDetailsScreen
import software.royya.watch.features.ponddetails.PondDetailsViewModel
import software.royya.watch.features.settings.SettingsScreen
import software.royya.watch.features.settings.SettingsViewModel

@Composable
fun RoyyaNavGraph(
    navController: NavHostController,
    startDestination: String = Screen.Home.route
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val showBottomBar = currentRoute != Screen.Onboarding.route &&
            currentRoute?.startsWith("alert_details") != true

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                BottomNavigationBar(navController = navController)
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(padding)
        ) {
            composable(Screen.Onboarding.route) {
                val viewModel: OnboardingViewModel = hiltViewModel()
                OnboardingScreen(
                    viewModel = viewModel,
                    onOnboardingFinished = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Onboarding.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.Home.route) {
                val viewModel: HomeViewModel = hiltViewModel()
                HomeScreen(
                    viewModel = viewModel,
                    onNavigateToAlertDetails = { alertId ->
                        navController.navigate(Screen.AlertDetails.createRoute(alertId))
                    },
                    onNavigateToPondDetails = { pondId ->
                        navController.navigate(Screen.PondDetails.createRoute(pondId))
                    },
                    onOpenAeratorControls = {
                        // Directly handled inside HomeScreen cards
                    }
                )
            }

            composable(
                route = Screen.PondDetails.route,
                arguments = listOf(navArgument("pondId") { type = NavType.StringType; defaultValue = "pond_01" })
            ) {
                val viewModel: PondDetailsViewModel = hiltViewModel()
                PondDetailsScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.Alerts.route) {
                val viewModel: AlertsViewModel = hiltViewModel()
                AlertsScreen(
                    viewModel = viewModel,
                    onNavigateToAlertDetails = { alertId ->
                        navController.navigate(Screen.AlertDetails.createRoute(alertId))
                    }
                )
            }

            composable(
                route = Screen.AlertDetails.route,
                arguments = listOf(navArgument("alertId") { type = NavType.StringType; defaultValue = "alert_1024" })
            ) {
                val viewModel: AlertDetailsViewModel = hiltViewModel()
                AlertDetailsScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.Settings.route) {
                val viewModel: SettingsViewModel = hiltViewModel()
                SettingsScreen(
                    viewModel = viewModel
                )
            }
        }
    }
}

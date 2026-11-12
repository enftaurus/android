package software.royya.watch.navigation

sealed class Screen(val route: String) {
    data object Onboarding : Screen("onboarding")
    data object Home : Screen("home")
    data object PondDetails : Screen("pond_details/{pondId}") {
        fun createRoute(pondId: String) = "pond_details/$pondId"
    }
    data object Alerts : Screen("alerts")
    data object AlertDetails : Screen("alert_details/{alertId}") {
        fun createRoute(alertId: String) = "alert_details/$alertId"
    }
    data object Settings : Screen("settings")
}

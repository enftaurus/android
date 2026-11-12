package software.royya.watch.navigation

import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import software.royya.watch.R
import software.royya.watch.core.design.RoyyaPrimaryDark
import software.royya.watch.core.design.RoyyaSoftGreen
import software.royya.watch.core.design.RoyyaSurface
import software.royya.watch.core.design.RoyyaTextSecondary

sealed class BottomNavItem(
    val route: String,
    val titleRes: Int,
    val icon: ImageVector
) {
    data object Home : BottomNavItem(Screen.Home.route, R.string.nav_home, Icons.Default.Home)
    data object Pond : BottomNavItem("pond_details/pond_01", R.string.nav_pond, Icons.Default.WaterDrop)
    data object Alerts : BottomNavItem(Screen.Alerts.route, R.string.nav_alerts, Icons.Default.Notifications)
    data object Settings : BottomNavItem(Screen.Settings.route, R.string.nav_settings, Icons.Default.Settings)
}

@Composable
fun BottomNavigationBar(navController: NavController) {
    val items = listOf(
        BottomNavItem.Home,
        BottomNavItem.Pond,
        BottomNavItem.Alerts,
        BottomNavItem.Settings
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    NavigationBar(
        containerColor = RoyyaSurface,
        tonalElevation = 8.dp,
        modifier = Modifier.height(72.dp)
    ) {
        items.forEach { item ->
            val isSelected = currentRoute == item.route ||
                (item is BottomNavItem.Pond && currentRoute?.startsWith("pond_details") == true)

            NavigationBarItem(
                icon = { Icon(item.icon, contentDescription = stringResource(item.titleRes)) },
                label = { Text(stringResource(item.titleRes), style = MaterialTheme.typography.labelSmall) },
                selected = isSelected,
                onClick = {
                    if (currentRoute != item.route) {
                        navController.navigate(item.route) {
                            popUpTo(Screen.Home.route) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = RoyyaPrimaryDark,
                    selectedTextColor = RoyyaPrimaryDark,
                    indicatorColor = RoyyaSoftGreen,
                    unselectedIconColor = RoyyaTextSecondary,
                    unselectedTextColor = RoyyaTextSecondary
                )
            )
        }
    }
}

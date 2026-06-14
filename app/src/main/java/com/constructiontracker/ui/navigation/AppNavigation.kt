package com.constructiontracker.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.constructiontracker.ui.screens.addpayment.AddPaymentScreen
import com.constructiontracker.ui.screens.addpurchase.AddPurchaseScreen
import com.constructiontracker.ui.screens.overview.OverviewScreen
import com.constructiontracker.ui.screens.records.RecordsScreen
import com.constructiontracker.ui.screens.setup.SetupScreen

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    object Overview : Screen("overview", "Overview", Icons.Filled.Home)
    object AddPayment : Screen("add_payment", "Payment", Icons.Filled.Payments)
    object AddPurchase : Screen("add_purchase", "Purchase", Icons.Filled.ShoppingCart)
    object Records : Screen("records", "Records", Icons.Filled.List)
    object Setup : Screen("setup", "Setup", Icons.Filled.Settings)
}

private val bottomNavItems = listOf(
    Screen.Overview, Screen.AddPayment, Screen.AddPurchase, Screen.Records, Screen.Setup
)

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                bottomNavItems.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = screen.label) },
                        label = { Text(screen.label) },
                        selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Overview.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Overview.route) { OverviewScreen() }
            composable(Screen.AddPayment.route) { AddPaymentScreen() }
            composable(Screen.AddPurchase.route) { AddPurchaseScreen() }
            composable(Screen.Records.route) { RecordsScreen() }
            composable(Screen.Setup.route) { SetupScreen() }
        }
    }
}

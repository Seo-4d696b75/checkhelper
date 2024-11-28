package com.seo4d696b75.android.ekisagasu.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import com.seo4d696b75.android.ekisagasu.ui.navigation.component.BottomNavigationBar

@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
) {
    val navController = rememberNavController()
    Scaffold(
        bottomBar = {
            BottomNavigationBar(navController = navController)
        },
        modifier = modifier.fillMaxSize(),
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = NavigationTab.Home,
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
        ) {
            navigation<NavigationTab.Home>(
                startDestination = NavigationRoute.Home.Top,
            ) {
                composable<NavigationRoute.Home.Top> {
                    Box(
                        modifier = modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "Home",
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                }
            }
            navigation<NavigationTab.Log>(
                startDestination = NavigationRoute.Log.Top,
            ) {
                composable<NavigationRoute.Log.Top> {
                    Box(
                        modifier = modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "Log",
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                }
            }
            navigation<NavigationTab.Setting>(
                startDestination = NavigationRoute.Setting.Top,
            ) {
                composable<NavigationRoute.Setting.Top> {
                    Box(
                        modifier = modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "Setting",
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                }
            }
        }
    }
}

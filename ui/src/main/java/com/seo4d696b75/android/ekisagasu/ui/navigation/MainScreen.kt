package com.seo4d696b75.android.ekisagasu.ui.navigation

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.seo4d696b75.android.ekisagasu.ui.home.HomeScreenShell
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
            val enter = fadeIn() + slideInHorizontally()
            val exit = fadeOut() + slideOutHorizontally()
            navigation<NavigationTab.Home>(
                startDestination = NavigationRoute.Home.Radar,
                enterTransition = { enter },
                popEnterTransition = { enter },
                exitTransition = { exit },
                popExitTransition = { exit },
            ) {
                composable<NavigationRoute.Home.Radar> {
                    HomeScreenShell {
                        Column(
                            modifier = modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Text(
                                text = "Home",
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Button(
                                onClick = {
                                    navController.navigate(NavigationRoute.Home.Station(1000101))
                                },
                            ) {
                                Text(text = "station")
                            }
                            Button(
                                onClick = {
                                    navController.navigate(NavigationRoute.Home.Line(10001))
                                },
                            ) {
                                Text(text = "line")
                            }
                        }
                    }
                }
                composable<NavigationRoute.Home.Station> { backstack ->
                    val args = backstack.toRoute<NavigationRoute.Home.Station>()
                    val owner = navController.getBackStackEntry<NavigationRoute.Home.Radar>()
                    HomeScreenShell(
                        viewModel = hiltViewModel(viewModelStoreOwner = owner),
                    ) {
                        Box(
                            modifier = modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "Station ${args.code}",
                                style = MaterialTheme.typography.titleMedium,
                            )
                        }
                    }
                }
                composable<NavigationRoute.Home.Line> { backstack ->
                    val args = backstack.toRoute<NavigationRoute.Home.Line>()
                    val owner = navController.getBackStackEntry<NavigationRoute.Home.Radar>()
                    HomeScreenShell(
                        viewModel = hiltViewModel(viewModelStoreOwner = owner),
                    ) {
                        Box(
                            modifier = modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "Line ${args.code}",
                                style = MaterialTheme.typography.titleMedium,
                            )
                        }
                    }
                }
            }
            navigation<NavigationTab.Log>(
                startDestination = NavigationRoute.Log.Top,
                enterTransition = { enter },
                popEnterTransition = { enter },
                exitTransition = { exit },
                popExitTransition = { exit },
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
                enterTransition = { enter },
                popEnterTransition = { enter },
                exitTransition = { exit },
                popExitTransition = { exit },
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

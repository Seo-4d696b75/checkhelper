package com.seo4d696b75.android.ekisagasu.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.seo4d696b75.android.ekisagasu.ui.home.HomeScreenShell
import com.seo4d696b75.android.ekisagasu.ui.navigation.component.BottomNavigationBar
import com.seo4d696b75.android.ekisagasu.ui.radar.RadarScreen
import com.seo4d696b75.android.ekisagasu.ui.setting.SettingScreen
import com.seo4d696b75.android.ekisagasu.ui.update.DataUpdateNavigationEvent
import com.seo4d696b75.android.ekisagasu.ui.update.dataUpdateDialog

@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
) {
    val navController = rememberNavController()

    DataUpdateNavigationEvent(navController)

    Scaffold(
        bottomBar = {
            BottomNavigationBar(navController = navController)
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        modifier = modifier.fillMaxSize(),
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = NavigationTab.Home,
            typeMap = typeMap,
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .consumeWindowInsets(
                    WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom)
                ),
        ) {
            dataUpdateDialog(navController)
            navigation<NavigationTab.Home>(
                startDestination = NavigationRoute.Home.Radar,
            ) {
                composable<NavigationRoute.Home.Radar> {
                    HomeScreenShell {
                        RadarScreen(
                            onStationClicked = {
                                navController.navigate(NavigationRoute.Home.Station(it.code))
                            },
                            modifier = Modifier.fillMaxSize(),
                        )
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
                    SettingScreen(
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
    }
}

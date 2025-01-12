package com.seo4d696b75.android.ekisagasu.ui.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.toRoute
import com.seo4d696b75.android.ekisagasu.ui.event.NavigationEvent
import com.seo4d696b75.android.ekisagasu.ui.navigation.NavigationRoute
import com.seo4d696b75.android.ekisagasu.ui.navigation.NavigationTab
import com.seo4d696b75.android.ekisagasu.ui.navigation.composable
import com.seo4d696b75.android.ekisagasu.ui.navigation.navigation
import com.seo4d696b75.android.ekisagasu.ui.radar.RadarScreen
import com.seo4d696b75.android.ekisagasu.ui.radar.RadarViewModel

fun NavGraphBuilder.homeNavigation(navController: NavController) {
    navigation<NavigationTab.Home>(
        startDestination = NavigationRoute.Home.Radar,
    ) {
        composable<NavigationRoute.Home.Radar> {
            HomeScreenShell {
                val viewModel: RadarViewModel = hiltViewModel()
                NavigationEvent(viewModel) {
                    navController.navigate(NavigationRoute.Home.Station(it.station.code))
                }
                RadarScreen(
                    viewModel = viewModel,
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
                    modifier = Modifier.fillMaxSize(),
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
                    modifier = Modifier.fillMaxSize(),
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
}

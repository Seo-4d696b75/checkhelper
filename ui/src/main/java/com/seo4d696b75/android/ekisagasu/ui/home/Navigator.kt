package com.seo4d696b75.android.ekisagasu.ui.home

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import com.seo4d696b75.android.ekisagasu.ui.R
import com.seo4d696b75.android.ekisagasu.ui.event.NavigationEvent
import com.seo4d696b75.android.ekisagasu.ui.line.LineScreen
import com.seo4d696b75.android.ekisagasu.ui.line.LineViewModel
import com.seo4d696b75.android.ekisagasu.ui.navigation.NavigationRoute
import com.seo4d696b75.android.ekisagasu.ui.navigation.NavigationTab
import com.seo4d696b75.android.ekisagasu.ui.navigation.composable
import com.seo4d696b75.android.ekisagasu.ui.navigation.navigation
import com.seo4d696b75.android.ekisagasu.ui.radar.RadarScreen
import com.seo4d696b75.android.ekisagasu.ui.radar.RadarViewModel
import com.seo4d696b75.android.ekisagasu.ui.station.StationScreen
import com.seo4d696b75.android.ekisagasu.ui.station.StationViewModel

fun NavGraphBuilder.homeNavigation(navController: NavController) {
    navigation<NavigationTab.Home>(
        startDestination = NavigationRoute.Home.Radar,
    ) {
        composable<NavigationRoute.Home.Radar> {
            val viewModel: RadarViewModel = hiltViewModel()
            NavigationEvent(viewModel) {
                navController.navigate(NavigationRoute.Home.Station(it.station.code))
            }
            RadarScreen(
                viewModel = viewModel,
                modifier = Modifier.fillMaxSize(),
            )
        }
        composable<NavigationRoute.Home.Station> {
            val viewModel: StationViewModel = hiltViewModel()
            val context = LocalContext.current
            NavigationEvent(viewModel) {
                when (it) {
                    StationViewModel.Nav.Close ->
                        navController.popBackStack(NavigationRoute.Home.Radar, false)

                    is StationViewModel.Nav.ShowMap -> {
                        val intent = Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse(context.getString(R.string.map_url) + "?station=${it.station.code}"),
                        )
                        context.startActivity(intent)
                    }

                    is StationViewModel.Nav.ShowLine -> {
                        val route = NavigationRoute.Home.Line(it.line.code)
                        navController.navigate(route)
                    }
                }
            }
            StationScreen(viewModel = viewModel)
        }
        composable<NavigationRoute.Home.Line> {
            val viewModel: LineViewModel = hiltViewModel()
            val context = LocalContext.current
            NavigationEvent(viewModel) {
                when (it) {
                    LineViewModel.Nav.Close ->
                        navController.popBackStack(NavigationRoute.Home.Radar, false)

                    is LineViewModel.Nav.ShowMap -> {
                        val intent = Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse(context.getString(R.string.map_url) + "?line=${it.line.code}"),
                        )
                        context.startActivity(intent)
                    }

                    is LineViewModel.Nav.ShowStation -> {
                        val route = NavigationRoute.Home.Station(it.station.code)
                        navController.navigate(route)
                    }
                }
            }
            LineScreen(viewModel = viewModel)
        }
    }
}

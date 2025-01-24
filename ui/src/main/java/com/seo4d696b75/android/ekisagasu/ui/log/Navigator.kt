package com.seo4d696b75.android.ekisagasu.ui.log

import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import com.seo4d696b75.android.ekisagasu.ui.navigation.NavigationRoute
import com.seo4d696b75.android.ekisagasu.ui.navigation.NavigationTab
import com.seo4d696b75.android.ekisagasu.ui.navigation.composable
import com.seo4d696b75.android.ekisagasu.ui.navigation.navigation

fun NavGraphBuilder.logNavigation(navController: NavController) {
    navigation<NavigationTab.Log>(
        startDestination = NavigationRoute.Log.Top,
    ) {
        composable<NavigationRoute.Log.Top> {
            val viewModel: LogComposeViewModel = hiltViewModel()
            LogScreen(viewModel = viewModel)
        }
    }
}

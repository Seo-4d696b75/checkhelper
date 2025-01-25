package com.seo4d696b75.android.ekisagasu.ui.log

import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.dialog
import com.seo4d696b75.android.ekisagasu.ui.event.NavigationEvent
import com.seo4d696b75.android.ekisagasu.ui.log.history.LogHistoryDialog
import com.seo4d696b75.android.ekisagasu.ui.log.history.LogHistoryViewModel
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
            NavigationEvent(viewModel) {
                when (it) {
                    LogComposeViewModel.Nav.SelectLogTarget -> navController.navigate(NavigationRoute.Log.HistoryDialog)
                }
            }

            LogScreen(viewModel = viewModel)
        }

        dialog<NavigationRoute.Log.HistoryDialog> {
            val viewModel: LogHistoryViewModel = hiltViewModel()
            NavigationEvent(viewModel) {
                navController.popBackStack()
            }

            LogHistoryDialog(viewModel = viewModel)
        }
    }
}

package com.seo4d696b75.android.ekisagasu.ui.log

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Log",
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }
    }
}

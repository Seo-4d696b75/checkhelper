package com.seo4d696b75.android.ekisagasu.ui.setting

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraphBuilder
import com.seo4d696b75.android.ekisagasu.ui.navigation.NavigationRoute
import com.seo4d696b75.android.ekisagasu.ui.navigation.NavigationTab
import com.seo4d696b75.android.ekisagasu.ui.navigation.composable
import com.seo4d696b75.android.ekisagasu.ui.navigation.navigation

fun NavGraphBuilder.settingNavigation() {
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

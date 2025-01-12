package com.seo4d696b75.android.ekisagasu.ui.navigation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.seo4d696b75.android.ekisagasu.ui.error.ErrorDialog
import com.seo4d696b75.android.ekisagasu.ui.home.homeNavigation
import com.seo4d696b75.android.ekisagasu.ui.log.logNavigation
import com.seo4d696b75.android.ekisagasu.ui.navigation.component.BottomNavigationBar
import com.seo4d696b75.android.ekisagasu.ui.setting.settingNavigation
import com.seo4d696b75.android.ekisagasu.ui.update.DataUpdateNavigationEvent
import com.seo4d696b75.android.ekisagasu.ui.update.dataUpdateDialog

@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
) {
    val navController = rememberNavController()

    DataUpdateNavigationEvent(navController)

    ErrorDialog()

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
            homeNavigation(navController)
            logNavigation(navController)
            settingNavigation()
        }
    }
}

package com.seo4d696b75.android.ekisagasu.ui.navigation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.seo4d696b75.android.ekisagasu.ui.error.ErrorHandler
import com.seo4d696b75.android.ekisagasu.ui.home.HomeScreenShell
import com.seo4d696b75.android.ekisagasu.ui.home.homeNavigation
import com.seo4d696b75.android.ekisagasu.ui.log.logNavigation
import com.seo4d696b75.android.ekisagasu.ui.navigation.component.BottomNavigationBar
import com.seo4d696b75.android.ekisagasu.ui.permission.PermissionHandler
import com.seo4d696b75.android.ekisagasu.ui.permission.PermissionViewModel
import com.seo4d696b75.android.ekisagasu.ui.selectLine.SelectLineNavigationEvent
import com.seo4d696b75.android.ekisagasu.ui.selectLine.lineSelectDialog
import com.seo4d696b75.android.ekisagasu.ui.setting.settingNavigation
import com.seo4d696b75.android.ekisagasu.ui.update.DataUpdateNavigationEvent
import com.seo4d696b75.android.ekisagasu.ui.update.dataUpdateDialog

@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
    onPermissionReady: () -> Unit,
) {
    val navController = rememberNavController()

    val permissionViewModel: PermissionViewModel = hiltViewModel()
    PermissionHandler(
        viewModel = permissionViewModel,
        onReady = onPermissionReady,
    )

    DataUpdateNavigationEvent(navController)
    SelectLineNavigationEvent(navController)

    ErrorHandler(
        onGMSResolutionResult = permissionViewModel::onDeviceLocationSettingResult,
    )

    Scaffold(
        bottomBar = {
            BottomNavigationBar(navController = navController)
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        modifier = modifier.fillMaxSize(),
    ) { padding ->
        HomeScreenShell(
            navController = navController,
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .consumeWindowInsets(WindowInsets.navigationBars),
        ) {
            NavHost(
                navController = navController,
                startDestination = NavigationTab.Home,
                typeMap = typeMap,
            ) {
                dataUpdateDialog(navController)
                homeNavigation(navController)
                logNavigation(navController)
                settingNavigation()
                lineSelectDialog(navController)
            }
        }
    }
}

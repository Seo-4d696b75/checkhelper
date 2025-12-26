package com.seo4d696b75.android.ekisagasu.ui.log

import android.app.Activity
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.dialog
import com.seo4d696b75.android.ekisagasu.ui.event.NavigationEvent
import com.seo4d696b75.android.ekisagasu.ui.log.history.LogHistoryDialog
import com.seo4d696b75.android.ekisagasu.ui.log.history.LogHistoryViewModel
import com.seo4d696b75.android.ekisagasu.ui.log.output.LogOutputConfigDialog
import com.seo4d696b75.android.ekisagasu.ui.log.output.LogOutputConfigViewModel
import com.seo4d696b75.android.ekisagasu.ui.navigation.NavigationRoute
import com.seo4d696b75.android.ekisagasu.ui.navigation.NavigationTab
import com.seo4d696b75.android.ekisagasu.ui.navigation.composable
import com.seo4d696b75.android.ekisagasu.ui.navigation.navigation
import com.seo4d696b75.android.ekisagasu.ui.navigation.typeMap
import com.seo4d696b75.android.ekisagasu.ui.utils.rememberLauncherForActivityResult
import timber.log.Timber

fun NavGraphBuilder.logNavigation(navController: NavController) {
    navigation<NavigationTab.Log>(
        startDestination = NavigationRoute.Log.Top,
    ) {
        composable<NavigationRoute.Log.Top> {
            val viewModel: LogViewModel = hiltViewModel()

            val launcher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.StartActivityForResult(),
            )
            val context = LocalContext.current
            NavigationEvent(viewModel) {
                when (it) {
                    LogViewModel.Nav.SelectLogTarget ->
                        navController.navigate(NavigationRoute.Log.HistoryDialog)

                    is LogViewModel.Nav.RequestOutputFile -> {
                        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                            addCategory(Intent.CATEGORY_OPENABLE)
                            this.type = "text/*"
                            putExtra(Intent.EXTRA_TITLE, it.config.fineName)
                        }
                        launcher.launch(intent) { result ->
                            val uri = result.data?.data
                            if (result.resultCode == Activity.RESULT_OK && uri != null) {
                                Timber.d("log file resoled: $uri")
                                viewModel.writeLogFile(it.config, uri, context.contentResolver)
                            }
                        }
                    }

                    is LogViewModel.Nav.ConfigureOutputFile -> {
                        val route = NavigationRoute.Log.LogOutputConfigDialog(it.config)
                        navController.navigate(route)
                    }
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

        dialog<NavigationRoute.Log.LogOutputConfigDialog>(typeMap) {
            val viewModel: LogOutputConfigViewModel = hiltViewModel()
            val logViewModel: LogViewModel = hiltViewModel(
                viewModelStoreOwner = navController.getBackStackEntry<NavigationRoute.Log.Top>(),
            )
            NavigationEvent(viewModel) {
                when (it) {
                    LogOutputConfigViewModel.Nav.Cancel -> navController.popBackStack()

                    is LogOutputConfigViewModel.Nav.WriteLog -> {
                        navController.popBackStack()
                        logViewModel.onLogOutputConfigured(it.config)
                    }
                }
            }
            LogOutputConfigDialog(viewModel = viewModel)
        }
    }
}

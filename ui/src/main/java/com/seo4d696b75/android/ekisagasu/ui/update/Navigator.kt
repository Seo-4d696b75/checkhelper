package com.seo4d696b75.android.ekisagasu.ui.update

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.dialog
import com.seo4d696b75.android.ekisagasu.ui.event.NavigationEvent
import com.seo4d696b75.android.ekisagasu.ui.navigation.NavigationRoute
import com.seo4d696b75.android.ekisagasu.ui.navigation.typeMap
import com.seo4d696b75.android.ekisagasu.ui.update.confirm.ConfirmDataUpdateDialog
import com.seo4d696b75.android.ekisagasu.ui.update.confirm.ConfirmDataUpdateViewModel
import com.seo4d696b75.android.ekisagasu.ui.update.execute.DataUpdateDialog
import com.seo4d696b75.android.ekisagasu.ui.update.execute.DataUpdateViewModel
import com.seo4d696b75.android.ekisagasu.ui.update.retry.RetryDataUpdateDialog
import com.seo4d696b75.android.ekisagasu.ui.update.retry.RetryDataUpdateViewModel
import com.seo4d696b75.android.ekisagasu.ui.update.success.DataUpdateSuccessDialog

fun NavGraphBuilder.dataUpdateDialog(controller: NavController) {
    dialog<NavigationRoute.DataUpdate.ConfirmDialog>(typeMap) {
        val viewModel: ConfirmDataUpdateViewModel = hiltViewModel()
        NavigationEvent(viewModel) {
            controller.popBackStack()
            when (it) {
                ConfirmDataUpdateViewModel.Nav.CancelUpdate -> {}

                is ConfirmDataUpdateViewModel.Nav.ExecuteUpdate -> {
                    val route = NavigationRoute.DataUpdate.ExecuteDialog(it.type, it.info)
                    controller.navigate(route)
                }
            }
        }
        ConfirmDataUpdateDialog(
            type = viewModel.args.type,
            info = viewModel.args.info,
            onResult = viewModel::onResult,
        )
    }

    dialog<NavigationRoute.DataUpdate.ExecuteDialog>(typeMap) {
        val viewModel: DataUpdateViewModel = hiltViewModel()
        LaunchedEffect(viewModel) {
            viewModel.update()
        }
        NavigationEvent(viewModel) {
            controller.popBackStack()
            when (it) {
                DataUpdateViewModel.Nav.Success -> controller.navigate(NavigationRoute.DataUpdate.SuccessDialog)

                is DataUpdateViewModel.Nav.Failure -> {
                    val route = NavigationRoute.DataUpdate.RetryDialog(it.type, it.info)
                    controller.navigate(route)
                }

                DataUpdateViewModel.Nav.Cancel -> {}
            }
        }

        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        DataUpdateDialog(
            state = uiState,
            onCancel = viewModel::cancel,
        )
    }

    dialog<NavigationRoute.DataUpdate.SuccessDialog> {
        DataUpdateSuccessDialog(
            onClose = controller::popBackStack
        )
    }

    dialog<NavigationRoute.DataUpdate.RetryDialog>(typeMap) {
        val viewModel: RetryDataUpdateViewModel = hiltViewModel()
        NavigationEvent(viewModel) {
            controller.popBackStack()
            when (it) {
                is RetryDataUpdateViewModel.Nav.Retry -> {
                    val route = NavigationRoute.DataUpdate.ExecuteDialog(it.type, it.info)
                    controller.navigate(route)
                }

                RetryDataUpdateViewModel.Nav.Cancel -> {}
            }
        }
        RetryDataUpdateDialog(
            onRetry = viewModel::retry,
            onCancel = viewModel::cancel,
        )
    }
}

package com.seo4d696b75.android.ekisagasu.ui.update

import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.dialog
import com.seo4d696b75.android.ekisagasu.ui.navigation.NavigationRoute
import com.seo4d696b75.android.ekisagasu.ui.navigation.typeMap
import com.seo4d696b75.android.ekisagasu.ui.update.confirm.ConfirmDataUpdateDialog
import com.seo4d696b75.android.ekisagasu.ui.update.confirm.ConfirmDataUpdateViewModel

fun NavGraphBuilder.dataUpdateDialog(controller: NavController) {
    dialog<NavigationRoute.DataUpdate.ConfirmUpdateDialog>(typeMap = typeMap) {
        val viewModel: ConfirmDataUpdateViewModel = hiltViewModel()
        ConfirmDataUpdateDialog(
            type = viewModel.args.type,
            info = viewModel.args.info,
            onClose = controller::popBackStack,
            onResult = viewModel::onResult,
        )
    }

    dialog<NavigationRoute.DataUpdate.UpdateDialog>(typeMap = typeMap) {

    }
    dialog<NavigationRoute.DataUpdate.UpdateSuccessDialog>(typeMap = typeMap) {

    }
}

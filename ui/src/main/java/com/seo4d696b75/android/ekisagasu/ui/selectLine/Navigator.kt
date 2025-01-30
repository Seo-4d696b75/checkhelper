package com.seo4d696b75.android.ekisagasu.ui.selectLine

import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.dialog
import com.seo4d696b75.android.ekisagasu.ui.event.NavigationEvent
import com.seo4d696b75.android.ekisagasu.ui.navigation.NavigationRoute
import com.seo4d696b75.android.ekisagasu.ui.navigation.typeMap

fun NavGraphBuilder.lineSelectDialog(
    navController: NavController,
) {
    dialog<NavigationRoute.SelectLineDialog>(typeMap) {
        val viewModel: LineSelectViewModel = hiltViewModel()
        NavigationEvent(viewModel) {
            when (it) {
                LineSelectViewModel.Nav.Close -> navController.popBackStack()
            }
        }
        LineSelectDialog(viewModel)
    }
}

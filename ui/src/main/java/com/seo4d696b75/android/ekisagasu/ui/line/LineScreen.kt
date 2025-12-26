package com.seo4d696b75.android.ekisagasu.ui.line

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.seo4d696b75.android.ekisagasu.domain.dataset.Station
import com.seo4d696b75.android.ekisagasu.ui.line.section.LineDetailSection

@Composable
fun LineScreen(
    modifier: Modifier = Modifier,
    viewModel: LineViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LineScreen(
        state = state,
        onClose = viewModel::onClose,
        onMapClicked = viewModel::onMapClicked,
        onStationClicked = viewModel::onStationClicked,
        modifier = modifier.fillMaxSize(),
    )
}

@Composable
fun LineScreen(
    state: LineUiState,
    onClose: () -> Unit,
    onMapClicked: () -> Unit,
    onStationClicked: (Station) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (state is LineUiState.Loaded) {
        LineDetailSection(
            state = state,
            onClose = onClose,
            onMapClicked = onMapClicked,
            onStationClicked = onStationClicked,
            modifier = modifier,
        )
    }
}

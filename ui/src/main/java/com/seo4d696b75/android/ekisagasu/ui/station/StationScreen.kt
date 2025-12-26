package com.seo4d696b75.android.ekisagasu.ui.station

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.seo4d696b75.android.ekisagasu.domain.dataset.Line
import com.seo4d696b75.android.ekisagasu.ui.station.section.StationDetailSection

@Composable
fun StationScreen(
    modifier: Modifier = Modifier,
    viewModel: StationViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    StationScreen(
        state = state,
        onClose = viewModel::onClose,
        onMapClicked = viewModel::onMapClicked,
        onLineClicked = viewModel::onLineClicked,
        modifier = modifier,
    )
}

@Composable
fun StationScreen(
    state: StationUiState,
    onClose: () -> Unit,
    onMapClicked: () -> Unit,
    onLineClicked: (Line) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (state is StationUiState.Loaded) {
        StationDetailSection(
            modifier = modifier.fillMaxSize(),
            state = state,
            onClose = onClose,
            onMapClicked = onMapClicked,
            onLineClicked = onLineClicked,
        )
    }
}

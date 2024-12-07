package com.seo4d696b75.android.ekisagasu.ui.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.seo4d696b75.android.ekisagasu.ui.home.section.HomeSection

@Composable
fun HomeScreenShell(
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
    content: @Composable () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(8.dp),
    ) {
        HomeSection(
            state = state,
            modifier = Modifier.fillMaxWidth(),
        )
        content()
    }
}

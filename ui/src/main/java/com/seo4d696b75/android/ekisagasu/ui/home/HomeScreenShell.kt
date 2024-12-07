package com.seo4d696b75.android.ekisagasu.ui.home

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.seo4d696b75.android.ekisagasu.ui.R
import com.seo4d696b75.android.ekisagasu.ui.home.section.HomeSection

@Composable
fun HomeScreenShell(
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
    content: @Composable () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    HomeScreenShell(
        state = state,
        onSearchStateChanged = viewModel::onSearchStateChanged,
        content = content,
        modifier = modifier,
    )
}

@Composable
fun HomeScreenShell(
    state: HomeUiState,
    onSearchStateChanged: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomEnd,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
        ) {
            HomeSection(
                state = state,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(8.dp))
            content()
        }
        FloatingActionButton(
            onClick = onSearchStateChanged,
            modifier = Modifier.padding(16.dp),
        ) {
            Crossfade(
                targetState = state is HomeUiState.Running,
                label = "fab running",
            ) { running ->
                if (running) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_pause),
                        contentDescription = "stop",
                    )
                } else {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_play),
                        contentDescription = "start",
                    )
                }
            }
        }
    }
}

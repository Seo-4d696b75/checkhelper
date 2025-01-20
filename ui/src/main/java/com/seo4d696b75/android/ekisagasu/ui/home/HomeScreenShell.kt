package com.seo4d696b75.android.ekisagasu.ui.home

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.seo4d696b75.android.ekisagasu.domain.dataset.Line
import com.seo4d696b75.android.ekisagasu.domain.dataset.Station
import com.seo4d696b75.android.ekisagasu.ui.R
import com.seo4d696b75.android.ekisagasu.ui.event.NavigationEvent
import com.seo4d696b75.android.ekisagasu.ui.home.section.HomeActionButtonSection
import com.seo4d696b75.android.ekisagasu.ui.home.section.HomeSection
import com.seo4d696b75.android.ekisagasu.ui.line.LineSelectType
import com.seo4d696b75.android.ekisagasu.ui.navigation.NavigationRoute
import com.seo4d696b75.android.ekisagasu.ui.navigation.NavigationTab
import com.seo4d696b75.android.ekisagasu.ui.navigation.toRoute
import com.seo4d696b75.android.ekisagasu.ui.navigation.toTab
import com.seo4d696b75.android.ekisagasu.ui.theme.AppTheme

@Composable
fun HomeScreenShell(
    navController: NavController,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val viewModel: HomeViewModel = hiltViewModel()
    LaunchedEffect(viewModel) {
        navController.currentBackStackEntryFlow.collect {
            val tab = it.toRoute().toTab()
            if (tab != null) {
                viewModel.onVisibilityChanged(tab == NavigationTab.Home)
            }
        }
    }

    val context = LocalContext.current
    NavigationEvent(viewModel) {
        when (it) {
            is HomeViewModel.Nav.ShowStation -> {
                val route = NavigationRoute.Home.Station(it.station.code)
                navController.navigate(route)
            }

            is HomeViewModel.Nav.ShowLine -> {
                val route = NavigationRoute.Home.Line(it.line.code)
                navController.navigate(route)
            }

            HomeViewModel.Nav.SelectCurrentLine -> {
                navController.navigate(NavigationRoute.SelectLineDialog(LineSelectType.Current))
            }

            HomeViewModel.Nav.SelectNavigatorLine -> {
                navController.navigate(NavigationRoute.SelectLineDialog(LineSelectType.Navigator))
            }

            HomeViewModel.Nav.ShowMap -> {
                val intent = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse(context.getString(R.string.map_url)),
                )
                context.startActivity(intent)
            }
        }
    }

    val state by viewModel.uiState.collectAsStateWithLifecycle()

    HomeScreenShell(
        state = state,
        onSearchStateChanged = viewModel::onSearchStateChanged,
        onFinishClicked = viewModel::onFinishClicked,
        onSelectLineClicked = viewModel::onSelectLineClicked,
        onLineNavigatorClicked = viewModel::onLineNavigatorClicked,
        onTimerClicked = viewModel::onTimerClicked,
        onMapClicked = viewModel::onMapClicked,
        onStationClicked = viewModel::onStationClicked,
        onLineClicked = viewModel::onLineClicked,
        content = content,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreenShell(
    state: HomeUiState,
    onSearchStateChanged: () -> Unit,
    onFinishClicked: () -> Unit,
    onSelectLineClicked: () -> Unit,
    onLineNavigatorClicked: () -> Unit,
    onTimerClicked: () -> Unit,
    onMapClicked: () -> Unit,
    onStationClicked: (Station) -> Unit,
    onLineClicked: (Line) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    if (state !is HomeUiState.Visible) {
        Box(
            modifier = modifier.fillMaxSize(),
        ) {
            content()
        }
        return
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(text = stringResource(id = R.string.app_name))
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    scrolledContainerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            contentAlignment = Alignment.BottomEnd,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
            ) {
                HomeSection(
                    state = state,
                    onStationClicked = onStationClicked,
                    onLineClicked = onLineClicked,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(16.dp))
                content()
            }
            HomeActionButtonSection(
                isRunning = state is HomeUiState.Running,
                onSearchStateChanged = onSearchStateChanged,
                onFinishClicked = onFinishClicked,
                onSelectLineClicked = onSelectLineClicked,
                onLineNavigatorClicked = onLineNavigatorClicked,
                onTimerClicked = onTimerClicked,
                onMapClicked = onMapClicked,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
@PreviewLightDark
private fun HomeScreenShellPreview() {
    AppTheme {
        HomeScreenShell(
            state = HomeUiState.Idle,
            onSearchStateChanged = {},
            onFinishClicked = {},
            onSelectLineClicked = {},
            onLineNavigatorClicked = {},
            onTimerClicked = {},
            onMapClicked = {},
            onStationClicked = {},
            onLineClicked = {},
        ) {

        }
    }
}

package com.seo4d696b75.android.ekisagasu.ui.log

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.seo4d696b75.android.ekisagasu.domain.log.AppLog
import com.seo4d696b75.android.ekisagasu.domain.log.AppLogTarget
import com.seo4d696b75.android.ekisagasu.domain.log.AppLogType
import com.seo4d696b75.android.ekisagasu.ui.R
import com.seo4d696b75.android.ekisagasu.ui.log.section.LogSection
import com.seo4d696b75.android.ekisagasu.ui.theme.AppTheme
import kotlinx.collections.immutable.toPersistentList
import java.util.Date

@Composable
fun LogScreen(
    modifier: Modifier = Modifier,
    viewModel: LogComposeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LogScreen(
        modifier = modifier,
        state = state,
        onSelectTargetClicked = viewModel::onSelectTargetClicked,
        onFilterChanged = viewModel::onFilterChanged,
        onSaveClicked = viewModel::onSaveClicked,
    )
}

@Composable
fun LogScreen(
    state: LogUiState,
    onSelectTargetClicked: () -> Unit,
    onFilterChanged: (AppLogType.Filter) -> Unit,
    onSaveClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (state is LogUiState.Loaded) {
        Scaffold(
            floatingActionButton = {
                FloatingActionButton(onClick = onSaveClicked) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_save),
                        contentDescription = "save logs",
                    )
                }
            },
            modifier = modifier
                .fillMaxSize()
                .safeDrawingPadding(),
        ) {
            LogSection(
                state = state,
                onSelectTargetClicked = onSelectTargetClicked,
                onFilterChanged = onFilterChanged,
                modifier = Modifier.padding(it),
            )
        }
    }
}

@Composable
@PreviewLightDark
private fun LogScreenPreview() {
    AppTheme {
        LogScreen(
            state = LogUiState.Loaded(
                filter = AppLogType.Filter.All,
                target = AppLogTarget(
                    id = 1L,
                    range = 1L..100L,
                    start = Date(),
                    end = Date(),
                    hasError = false,
                ),
                logs = List(40) {
                    AppLog(
                        id = it.toLong(),
                        type = AppLogType.System,
                        message = "log message",
                        timestamp = Date(),
                    )
                }.toPersistentList(),
            ),
            onSelectTargetClicked = {},
            onFilterChanged = {},
            onSaveClicked = {},
        )
    }
}

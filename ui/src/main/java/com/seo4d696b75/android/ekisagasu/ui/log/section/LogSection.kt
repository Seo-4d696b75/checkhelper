package com.seo4d696b75.android.ekisagasu.ui.log.section

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Surface
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.seo4d696b75.android.ekisagasu.domain.log.AppLog
import com.seo4d696b75.android.ekisagasu.domain.log.AppLogTarget
import com.seo4d696b75.android.ekisagasu.domain.log.AppLogType
import com.seo4d696b75.android.ekisagasu.ui.common.Collapsing
import com.seo4d696b75.android.ekisagasu.ui.log.LogUiState
import com.seo4d696b75.android.ekisagasu.ui.theme.AppTheme
import kotlinx.collections.immutable.toPersistentList
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogSection(
    state: LogUiState.Loaded,
    onSelectTargetClicked: () -> Unit,
    onFilterChanged: (AppLogType.Filter) -> Unit,
    modifier: Modifier = Modifier,
) {
    val behavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    Column(
        modifier = modifier
            .padding(horizontal = 16.dp)
            .nestedScroll(behavior.nestedScrollConnection),
    ) {
        Collapsing(
            scrollBehavior = behavior,
        ) {
            LogFilterSection(
                state = state,
                onSelectTargetClicked = onSelectTargetClicked,
                onFilterChanged = onFilterChanged,
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(vertical = 16.dp)
                    .fillMaxWidth(),
            )
        }
        LogListSection(
            logs = state.logs,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
@PreviewLightDark
private fun LogSectionPreview() {
    AppTheme {
        Surface {
            LogSection(
                state = LogUiState.Loaded(
                    filter = AppLogType.Filter.All,
                    target = AppLogTarget(
                        id = 1L,
                        range = 1L..100L,
                        start = Date(),
                        end = Date(),
                        hasError = false,
                    ),
                    logs = List(20) {
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
            )
        }
    }
}

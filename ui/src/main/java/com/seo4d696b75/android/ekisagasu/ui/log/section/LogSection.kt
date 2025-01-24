package com.seo4d696b75.android.ekisagasu.ui.log.section

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.seo4d696b75.android.ekisagasu.domain.date.TIME_PATTERN_DATETIME
import com.seo4d696b75.android.ekisagasu.domain.date.format
import com.seo4d696b75.android.ekisagasu.domain.log.AppLog
import com.seo4d696b75.android.ekisagasu.domain.log.AppLogTarget
import com.seo4d696b75.android.ekisagasu.domain.log.AppLogType
import com.seo4d696b75.android.ekisagasu.ui.R
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
    Column(
        modifier = modifier.padding(16.dp),
    ) {
        Row(
            verticalAlignment = Alignment.Bottom,
            modifier = Modifier.height(IntrinsicSize.Max),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .weight(1f)
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline,
                        shape = RoundedCornerShape(4.dp),
                    )
                    .clickable(onClick = onSelectTargetClicked)
                    .padding(8.dp),
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_calendar),
                    contentDescription = null,
                    modifier = Modifier.padding(end = 16.dp)
                )
                Text(
                    text = state.target.run {
                        val from = start.format(TIME_PATTERN_DATETIME)
                        val to = end?.let { it.format(TIME_PATTERN_DATETIME) }
                            ?: stringResource(id = R.string.log_filter_until_now)
                        "$from\n〜 $to"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    textAlign = TextAlign.Start,
                )

            }
            Spacer(modifier = Modifier.width(16.dp))

            var expanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded },
                modifier = Modifier.width(120.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxSize()
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outline,
                            shape = RoundedCornerShape(4.dp),
                        )
                        .clickable { expanded = true }
                        .padding(8.dp),
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_filter),
                        contentDescription = null,
                        modifier = Modifier.padding(end = 8.dp),
                    )
                    Text(
                        text = state.filter.name(),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                ) {
                    AppLogType.Filter.entries.forEach {
                        DropdownMenuItem(
                            text = {
                                Text(text = it.name())
                            },
                            onClick = {
                                onFilterChanged(it)
                                expanded = false
                            },
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        LogListSection(
            logs = state.logs,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
private fun AppLogType.Filter.name(): String = stringResource(
    id = when (this) {
        AppLogType.Filter.All -> R.string.log_filter_name_all
        AppLogType.Filter.System -> R.string.log_filter_name_system
        AppLogType.Filter.Geo -> R.string.log_filter_name_geo
        AppLogType.Filter.Station -> R.string.log_filter_name_station
    }
)

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

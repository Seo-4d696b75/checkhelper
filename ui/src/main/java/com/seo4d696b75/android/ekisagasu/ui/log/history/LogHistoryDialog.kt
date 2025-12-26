package com.seo4d696b75.android.ekisagasu.ui.log.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.seo4d696b75.android.ekisagasu.domain.date.TIME_PATTERN_DATETIME
import com.seo4d696b75.android.ekisagasu.domain.date.format
import com.seo4d696b75.android.ekisagasu.domain.log.AppLogTarget
import com.seo4d696b75.android.ekisagasu.ui.R
import com.seo4d696b75.android.ekisagasu.ui.theme.AppTheme
import com.seo4d696b75.android.ekisagasu.ui.utils.duration
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toPersistentList
import java.util.Date

@Composable
fun LogHistoryDialog(
    modifier: Modifier = Modifier,
    viewModel: LogHistoryViewModel = hiltViewModel(),
) {
    val histories by viewModel.uiState.collectAsStateWithLifecycle()

    LogHistoryDialog(
        histories = histories,
        onTargetSelected = viewModel::onTargetSelected,
        onClose = viewModel::onClose,
        modifier = modifier,
    )
}

@Composable
fun LogHistoryDialog(
    histories: ImmutableList<AppLogTarget>,
    onTargetSelected: (AppLogTarget) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AlertDialog(
        modifier = modifier,
        onDismissRequest = onClose,
        confirmButton = {
            TextButton(onClick = onClose) {
                Text(text = stringResource(id = R.string.dialog_button_cancel))
            }
        },
        title = {
            Text(text = stringResource(id = R.string.dialog_title_history))
        },
        text = {
            Column {
                Text(text = stringResource(id = R.string.dialog_message_history))
                Spacer(modifier = Modifier.height(16.dp))
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp),
                ) {
                    itemsIndexed(
                        items = histories,
                        key = { _, history -> history.id },
                    ) { index, history ->
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onTargetSelected(history) }
                                    .padding(vertical = 4.dp),
                            ) {
                                Text(
                                    text = history.start.format(TIME_PATTERN_DATETIME),
                                    style = MaterialTheme.typography.bodyLarge,
                                    maxLines = 1,
                                    modifier = Modifier.alignByBaseline(),
                                )
                                if (history.end != null) {
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = history.duration(),
                                        style = MaterialTheme.typography.bodyMedium,
                                        maxLines = 1,
                                        modifier = Modifier.alignByBaseline(),
                                    )
                                }
                                if (index == 0) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                                        contentDescription = "has error",
                                        modifier = Modifier
                                            .padding(start = 8.dp, end = 2.dp)
                                            .size(20.dp),
                                        tint = MaterialTheme.colorScheme.secondary,
                                    )
                                    Text(
                                        text = stringResource(id = R.string.app_history_running),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.secondary,
                                        maxLines = 1,
                                        modifier = Modifier.alignByBaseline(),
                                    )
                                }
                                if (history.hasError) {
                                    Icon(
                                        imageVector = Icons.Outlined.Warning,
                                        contentDescription = "has error",
                                        modifier = Modifier
                                            .padding(start = 8.dp, end = 2.dp)
                                            .size(20.dp),
                                        tint = MaterialTheme.colorScheme.error,
                                    )
                                    Text(
                                        text = stringResource(id = R.string.app_history_has_error),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.error,
                                        maxLines = 1,
                                        modifier = Modifier.alignByBaseline(),
                                    )
                                }
                            }
                            HorizontalDivider()
                        }
                    }
                }
            }
        },
    )
}

@Composable
@PreviewLightDark
private fun LogHistoryDialogPreview() {
    AppTheme {
        LogHistoryDialog(
            histories = List(40) {
                AppLogTarget(
                    id = it.toLong(),
                    range = (it * 10L)..(it * 10L + 9L),
                    start = Date(it * 1000_1000L),
                    end = Date((it + 1) * 1000_1000L),
                    hasError = it % 2 == 0,
                )
            }.toPersistentList(),
            onTargetSelected = {},
            onClose = {},
        )
    }
}

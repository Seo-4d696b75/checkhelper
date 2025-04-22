package com.seo4d696b75.android.ekisagasu.ui.selectLine

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.seo4d696b75.android.ekisagasu.domain.dataset.Line
import com.seo4d696b75.android.ekisagasu.ui.R
import com.seo4d696b75.android.ekisagasu.ui.selectLine.section.LineSelectSection
import com.seo4d696b75.android.ekisagasu.ui.theme.AppTheme
import com.seo4d696b75.android.ekisagasu.ui.utils.previewLines
import kotlinx.collections.immutable.toPersistentList

@Composable
fun LineSelectDialog(
    viewModel: LineSelectViewModel,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LineSelectDialog(
        uiState = state,
        onLineSelected = viewModel::onLineSelected,
        onLineCleared = viewModel::onLineCleared,
        onClose = viewModel::onCloseClicked,
        modifier = modifier,
    )
}

@Composable
fun LineSelectDialog(
    uiState: LineSelectUiState,
    onLineSelected: (Line) -> Unit,
    onLineCleared: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AlertDialog(
        onDismissRequest = onClose,
        title = {
            Text(text = stringResource(id = R.string.dialog_title_select_line))
        },
        confirmButton = {
            TextButton(onClick = onClose) {
                Text(text = stringResource(id = R.string.dialog_button_cancel))
            }
        },
        dismissButton = if (uiState.clearEnabled) {
            {
                TextButton(onClick = onLineCleared) {
                    Text(text = stringResource(id = R.string.dialog_button_unregister))
                }
            }
        } else {
            null
        },
        text = {
            LineSelectSection(
                message = uiState.message?.let { stringResource(id = it) },
                lines = uiState.lines,
                onLineSelected = onLineSelected,
            )
        },
        properties = DialogProperties(
            dismissOnClickOutside = true,
            dismissOnBackPress = true,
        ),
        modifier = modifier,
    )
}

@Composable
@PreviewLightDark
private fun LineSelectDialogPreview() {
    AppTheme {
        LineSelectDialog(
            uiState = LineSelectUiState(
                message = R.string.dialog_message_select_line,
                clearEnabled = true,
                lines = previewLines.toPersistentList(),
            ),
            onLineSelected = {},
            onLineCleared = { },
            onClose = { },
        )
    }
}

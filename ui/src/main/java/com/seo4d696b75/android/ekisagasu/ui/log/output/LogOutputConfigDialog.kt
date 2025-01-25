package com.seo4d696b75.android.ekisagasu.ui.log.output

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.seo4d696b75.android.ekisagasu.ui.R
import com.seo4d696b75.android.ekisagasu.ui.log.LogOutputExtension
import com.seo4d696b75.android.ekisagasu.ui.theme.AppTheme

@Composable
fun LogOutputConfigDialog(
    modifier: Modifier = Modifier,
    viewModel: LogOutputConfigComposeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LogOutputConfigDialog(
        extension = state,
        onExtensionChanged = viewModel::onExtensionChanged,
        onCancel = viewModel::onCancel,
        onWrite = viewModel::onWriteClicked,
        modifier = modifier,
    )
}


@Composable
fun LogOutputConfigDialog(
    extension: LogOutputExtension,
    onExtensionChanged: (LogOutputExtension) -> Unit,
    onCancel: () -> Unit,
    onWrite: () -> Unit,
    modifier: Modifier = Modifier,
) {

    AlertDialog(
        modifier = modifier,
        onDismissRequest = onCancel,
        confirmButton = {
            TextButton(onClick = onWrite) {
                Text(text = stringResource(id = R.string.dialog_button_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel) {
                Text(text = stringResource(id = R.string.dialog_button_cancel))
            }
        },
        title = {
            Text(text = stringResource(id = R.string.dialog_log_output_config_title))
        },
        text = {
            Column {
                Text(text = stringResource(id = R.string.dialog_log_output_config_message))
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = extension == LogOutputExtension.TXT,
                            onClick = { onExtensionChanged(LogOutputExtension.TXT) },
                            role = Role.RadioButton,
                        ),
                ) {
                    RadioButton(
                        selected = extension == LogOutputExtension.TXT,
                        onClick = null,
                    )
                    Column {
                        Text(
                            text = stringResource(id = R.string.file_extension_txt),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        Text(
                            text = stringResource(id = R.string.dialog_log_output_config_txt_description),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = extension == LogOutputExtension.GPX,
                            onClick = { onExtensionChanged(LogOutputExtension.GPX) },
                            role = Role.RadioButton,
                        ),
                ) {
                    RadioButton(
                        selected = extension == LogOutputExtension.GPX,
                        onClick = null,
                    )
                    Column {
                        Text(
                            text = stringResource(id = R.string.file_extension_gpx),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        Text(
                            text = stringResource(id = R.string.dialog_log_output_config_gpx_description),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        },
    )
}

@Composable
@PreviewLightDark
private fun LogOutputConfigDialogPreview() {
    AppTheme {
        LogOutputConfigDialog(
            extension = LogOutputExtension.TXT,
            onExtensionChanged = {},
            onCancel = {},
            onWrite = {},
        )
    }
}

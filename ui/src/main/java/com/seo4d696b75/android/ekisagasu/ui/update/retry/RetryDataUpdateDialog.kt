package com.seo4d696b75.android.ekisagasu.ui.update.retry

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.window.DialogProperties
import com.seo4d696b75.android.ekisagasu.ui.R
import com.seo4d696b75.android.ekisagasu.ui.theme.AppTheme

@Composable
fun RetryDataUpdateDialog(
    onCancel: () -> Unit,
    onRetry: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = { },
        title = {
            Text(text = stringResource(id = R.string.dialog_title_retry_update))
        },
        confirmButton = {
            TextButton(onClick = onRetry) {
                Text(text = stringResource(id = R.string.dialog_button_retry))
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel) {
                Text(text = stringResource(id = R.string.dialog_button_negative))
            }
        },
        text = {
            Text(text = stringResource(id = R.string.dialog_message_retry_update))
        },
        properties = DialogProperties(
            dismissOnClickOutside = false,
            dismissOnBackPress = false,
        ),
    )
}

@Composable
@PreviewLightDark
private fun RetryDataUpdateDialogPreview() {
    AppTheme {
        RetryDataUpdateDialog(
            onCancel = {},
            onRetry = {},
        )
    }
}

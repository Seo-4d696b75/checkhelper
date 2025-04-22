package com.seo4d696b75.android.ekisagasu.ui.update.execute

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.seo4d696b75.android.ekisagasu.domain.dataset.update.DataUpdateProgress
import com.seo4d696b75.android.ekisagasu.ui.R
import com.seo4d696b75.android.ekisagasu.ui.theme.AppTheme

@Composable
fun DataUpdateDialog(
    state: DataUpdateProgress,
    onCancel: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = { },
        title = {
            Text(text = stringResource(id = R.string.dialog_title_update_data))
        },
        confirmButton = { },
        dismissButton = {
            TextButton(
                onClick = {
                    onCancel()
                },
            ) {
                Text(text = stringResource(id = R.string.dialog_button_negative))
            }
        },
        text = {
            DataUpdateDialogContent(state)
        },
        properties = DialogProperties(
            dismissOnClickOutside = false,
            dismissOnBackPress = false,
        ),
    )
}

@Composable
private fun DataUpdateDialogContent(
    state: DataUpdateProgress,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircularProgressIndicator()
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = when (state) {
                is DataUpdateProgress.Download -> stringResource(id = R.string.update_state_download, state.percent)
                is DataUpdateProgress.Save -> stringResource(id = R.string.update_state_save)
            },
            textAlign = TextAlign.Start,
            modifier = Modifier.width(200.dp),
        )
    }
}

@Composable
@PreviewLightDark
private fun DataUpdateDialogPreview() {
    AppTheme {
        DataUpdateDialog(
            state = DataUpdateProgress.Download(12),
            onCancel = {},
        )
    }
}

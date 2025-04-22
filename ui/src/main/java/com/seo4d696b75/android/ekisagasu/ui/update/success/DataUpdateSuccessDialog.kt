package com.seo4d696b75.android.ekisagasu.ui.update.success

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
fun DataUpdateSuccessDialog(
    onClose: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onClose,
        title = {
            Text(text = stringResource(id = R.string.dialog_title_success_update))
        },
        confirmButton = { },
        dismissButton = {
            TextButton(onClick = onClose) {
                Text(text = stringResource(id = R.string.dialog_button_close))
            }
        },
        text = {
            Text(text = stringResource(id = R.string.dialog_message_success_update))
        },
        properties = DialogProperties(
            dismissOnClickOutside = true,
            dismissOnBackPress = true,
        ),
    )
}

@Composable
@PreviewLightDark
private fun DataUpdateSuccessDialogPreview() {
    AppTheme {
        DataUpdateSuccessDialog {}
    }
}

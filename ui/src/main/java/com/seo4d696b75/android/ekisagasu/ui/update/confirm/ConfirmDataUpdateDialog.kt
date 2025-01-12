package com.seo4d696b75.android.ekisagasu.ui.update.confirm

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.seo4d696b75.android.ekisagasu.domain.dataset.LatestDataVersion
import com.seo4d696b75.android.ekisagasu.domain.dataset.update.DataUpdateType
import com.seo4d696b75.android.ekisagasu.ui.R
import com.seo4d696b75.android.ekisagasu.ui.theme.AppTheme

@Composable
fun ConfirmDataUpdateDialog(
    type: DataUpdateType,
    info: LatestDataVersion,
    onResult: (Boolean) -> Unit,
) {
    AlertDialog(
        onDismissRequest = { },
        title = {
            Text(
                text = stringResource(
                    id = when (type) {
                        DataUpdateType.Init -> R.string.dialog_title_init_data
                        DataUpdateType.Latest -> R.string.dialog_title_latest_data
                    }
                )
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onResult(true) },
            ) {
                Text(text = stringResource(id = R.string.dialog_button_download))
            }
        },
        dismissButton = {
            TextButton(
                onClick = { onResult(false) },
            ) {
                Text(text = stringResource(id = R.string.dialog_button_negative))
            }
        },
        text = {
            ConfirmDataUpdateDialogContent(type, info)
        },
        properties = DialogProperties(
            dismissOnClickOutside = false,
            dismissOnBackPress = false,
        ),
    )
}

@Composable
private fun ConfirmDataUpdateDialogContent(
    type: DataUpdateType,
    info: LatestDataVersion,
) {
    Column {
        Text(
            text = stringResource(
                id = when (type) {
                    DataUpdateType.Init -> R.string.dialog_message_init_data
                    DataUpdateType.Latest -> R.string.dialog_message_latest_data
                }
            )
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_download),
                contentDescription = null,
                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurfaceVariant),
                modifier = Modifier.size(80.dp),
            )
            Spacer(modifier = Modifier.width(20.dp))
            Column {
                Text(text = stringResource(id = R.string.text_data_version, info.version))
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = stringResource(id = R.string.text_data_size, info.fileSize()))
            }
        }
    }
}

@Composable
@PreviewLightDark
private fun ConfirmDataUpdateDialogPreview() {
    AppTheme {
        ConfirmDataUpdateDialog(
            type = DataUpdateType.Init,
            info = LatestDataVersion(20250101L, 4_000_000L),
            onResult = {},
        )
    }
}

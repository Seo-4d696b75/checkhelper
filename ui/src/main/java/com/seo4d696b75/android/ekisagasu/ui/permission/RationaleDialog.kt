package com.seo4d696b75.android.ekisagasu.ui.permission

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.seo4d696b75.android.ekisagasu.ui.R
import com.seo4d696b75.android.ekisagasu.ui.theme.AppTheme

@Composable
fun RationaleDialog(
    viewModel: PermissionViewModel = hiltViewModel(),
) {
    val target by viewModel.uiState.collectAsStateWithLifecycle()

    target?.let {
        RationaleDialog(
            rationale = it,
            onRequest = { viewModel.requestPermission(it) },
            onCancel = viewModel::onPermissionRequestCancelled,
        )
    }
}

@Composable
fun RationaleDialog(
    rationale: PermissionRationale,
    onRequest: () -> Unit,
    onCancel: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = { },
        confirmButton = {
            TextButton(onClick = onRequest) {
                Text(text = stringResource(id = R.string.dialog_button_next))
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel) {
                Text(text = stringResource(id = R.string.dialog_button_finish_app))
            }
        },
        title = {
            Text(text = stringResource(id = R.string.dialog_title_permission))
        },
        text = {
            Column {
                Text(text = rationale.topDescription())
                rationale.image()?.let {
                    Image(
                        painter = it,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 20.dp)
                    )
                    Text(
                        text = stringResource(id = R.string.dialog_guide_image_caption),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = rationale.requestDescription())
            }
        },
        properties = DialogProperties(
            dismissOnClickOutside = false,
            dismissOnBackPress = false,
        ),
        modifier = Modifier
            .safeDrawingPadding()
            .fillMaxWidth(),
    )
}

@Composable
private fun PermissionRationale.topDescription(): String = when (this) {
    is PermissionRationale.LocationPermission ->
        stringResource(R.string.dialog_location_permission_rationale)

    is PermissionRationale.NotificationPermission ->
        stringResource(R.string.dialog_notification_permission_rationale)

    PermissionRationale.NotificationChannel ->
        stringResource(R.string.dialog_notification_permission_rationale)

    PermissionRationale.DrawOverlay -> stringResource(R.string.dialog_draw_overlay_rationale)
}

@Composable
private fun PermissionRationale.image(): Painter? = when (this) {
    is PermissionRationale.LocationPermission -> if (showSystemRequestDialog) {
        null
    } else {
        painterResource(id = R.drawable.location_permission)
    }

    is PermissionRationale.NotificationPermission -> if (showSystemRequestDialog) {
        null
    } else {
        painterResource(id = R.drawable.notification_permission)
    }

    PermissionRationale.NotificationChannel -> painterResource(id = R.drawable.notification_permission)

    PermissionRationale.DrawOverlay -> painterResource(id = R.drawable.draw_overlay_setting)
}

@Composable
private fun PermissionRationale.requestDescription(): String = when (this) {
    is PermissionRationale.LocationPermission -> if (showSystemRequestDialog) {
        stringResource(R.string.dialog_location_permission_request_dialog)
    } else {
        stringResource(R.string.dialog_location_permission_request_setting)
    }

    is PermissionRationale.NotificationPermission -> if (showSystemRequestDialog) {
        stringResource(R.string.dialog_notification_permission_request_dialog)
    } else {
        stringResource(R.string.dialog_notification_permission_request_setting)
    }

    PermissionRationale.NotificationChannel -> stringResource(R.string.dialog_notification_channel_request)
    PermissionRationale.DrawOverlay -> stringResource(R.string.dialog_draw_overlay_request)
}

private class RationaleDialogPreviewParamProvider : PreviewParameterProvider<PermissionRationale> {
    override val values = sequenceOf(
        PermissionRationale.LocationPermission(true),
        PermissionRationale.LocationPermission(false),
        PermissionRationale.NotificationPermission(true),
        PermissionRationale.NotificationPermission(false),
        PermissionRationale.NotificationChannel,
        PermissionRationale.DrawOverlay,
    )
}

@Composable
@PreviewLightDark
private fun RationaleDialogPreview(
    @PreviewParameter(RationaleDialogPreviewParamProvider::class) rationale: PermissionRationale,
) {
    AppTheme {
        RationaleDialog(
            rationale = rationale,
            onRequest = {},
            onCancel = {},
        )
    }
}

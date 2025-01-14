package com.seo4d696b75.android.ekisagasu.ui.error

import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.common.api.ResolvableApiException
import com.seo4d696b75.android.ekisagasu.domain.error.AppException
import com.seo4d696b75.android.ekisagasu.domain.error.GMSResolvableException
import com.seo4d696b75.android.ekisagasu.ui.R
import com.seo4d696b75.android.ekisagasu.ui.permission.PermissionViewModel

@Composable
fun ErrorDialog() {
    val viewModel: ErrorViewModel = viewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()

    state?.let {
        if (it.error is AppException) {
            AppExceptionDialog(
                error = it.error,
                dismiss = viewModel::dismiss,
            )
        } else {
            RuntimeExceptionDialog(
                message = it.message,
                dismiss = viewModel::dismiss,
            )
        }
    }
}

@Composable
private fun AppExceptionDialog(
    error: AppException,
    dismiss: () -> Unit,
) {
    when (error) {
        is GMSResolvableException -> {
            val e = error.cause as? ResolvableApiException ?: return
            val activity = LocalContext.current as? ComponentActivity ?: return
            val viewModel: PermissionViewModel by remember(activity) { activity.viewModels() }
            val launcher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.StartIntentSenderForResult(),
                onResult = viewModel::onDeviceLocationSettingResult,
            )
            AlertDialog(
                onDismissRequest = dismiss,
                confirmButton = {
                    TextButton(
                        onClick = {
                            dismiss()
                            val request = IntentSenderRequest.Builder(e.resolution).build()
                            launcher.launch(request)
                        },
                    ) {
                        Text(text = stringResource(id = R.string.dialog_error_button_next))
                    }
                },
                title = {
                    Text(text = stringResource(id = R.string.dialog_error_title_default))
                },
                text = {
                    Text(text = stringResource(id = R.string.dialog_error_description_gms))
                },
                properties = DialogProperties(
                    dismissOnClickOutside = false,
                    dismissOnBackPress = false,
                ),
            )
        }
    }
}

@Composable
private fun RuntimeExceptionDialog(
    message: ErrorUiMessage,
    dismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = dismiss,
        confirmButton = {
            TextButton(onClick = dismiss) {
                Text(text = stringResource(id = R.string.dialog_error_button_close))
            }
        },
        title = {
            Text(text = message.title())
        },
        text = {
            Text(text = message.description())
        },
    )
}

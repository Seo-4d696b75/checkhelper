package com.seo4d696b75.android.ekisagasu.ui.error

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.common.api.ResolvableApiException
import com.seo4d696b75.android.ekisagasu.domain.error.AppException
import com.seo4d696b75.android.ekisagasu.domain.error.CheckLatestDataVersionException
import com.seo4d696b75.android.ekisagasu.domain.error.GMSResolvableException
import com.seo4d696b75.android.ekisagasu.domain.error.PolylineNotSupportedException
import com.seo4d696b75.android.ekisagasu.domain.error.UnavailableLocationException
import com.seo4d696b75.android.ekisagasu.ui.R

@Composable
fun ErrorHandler(
    onGMSResolutionResult: (ActivityResult) -> Unit,
) {
    val viewModel: ErrorViewModel = viewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult(),
        onResult = onGMSResolutionResult,
    )

    val error = state ?: return
    if (error is AppException) {
        when (error) {
            is GMSResolvableException -> {
                val cause = error.cause as? ResolvableApiException ?: return

                val request = IntentSenderRequest.Builder(cause.resolution).build()
                viewModel.dismiss()
                launcher.launch(request)
            }

            is UnavailableLocationException -> ErrorDialog(
                title = stringResource(id = R.string.dialog_error_title_default),
                description = stringResource(id = R.string.dialog_error_description_unavailable_location),
                dismiss = viewModel::dismiss,
            )

            is CheckLatestDataVersionException -> ErrorDialog(
                title = stringResource(id = R.string.dialog_error_title_default),
                description = stringResource(id = R.string.message_fail_fetch_latest_version),
                dismiss = viewModel::dismiss,
            )

            is PolylineNotSupportedException -> ErrorDialog(
                title = stringResource(id = R.string.dialog_error_title_default),
                description = stringResource(id = R.string.navigation_unsupported),
                dismiss = viewModel::dismiss,
            )
        }
    } else {
        ErrorDialog(
            title = stringResource(R.string.dialog_error_title_default),
            description = stringResource(id = R.string.dialog_error_description_default, error.message ?: ""),
            dismiss = viewModel::dismiss,
        )
    }
}

@Composable
private fun ErrorDialog(
    title: String,
    description: String,
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
            Text(text = title)
        },
        text = {
            Text(text = description)
        },
    )
}

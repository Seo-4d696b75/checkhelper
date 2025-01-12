package com.seo4d696b75.android.ekisagasu.ui.error

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.seo4d696b75.android.ekisagasu.ui.R

@Composable
fun ErrorDialog() {
    val viewModel: ErrorViewModel = viewModel()
    val message by viewModel.message.collectAsStateWithLifecycle()

    message?.let {
        AlertDialog(
            onDismissRequest = viewModel::dismiss,
            confirmButton = {
                TextButton(onClick = viewModel::dismiss) {
                    Text(text = stringResource(id = R.string.dialog_error_button_close))
                }
            },
            title = {
                Text(text = it.title())
            },
            text = {
                Text(text = it.description())
            },
        )
    }
}

package com.seo4d696b75.android.ekisagasu.ui.permission

import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.google.android.gms.common.GoogleApiAvailability
import com.seo4d696b75.android.ekisagasu.ui.R
import com.seo4d696b75.android.ekisagasu.ui.event.NavigationEvent
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.take

@Composable
fun PermissionHandler(
    viewModel: PermissionViewModel = hiltViewModel(),
    onReady: () -> Unit,
) {
    LifecycleEventEffect(event = Lifecycle.Event.ON_RESUME) {
        viewModel.check()
    }
    LaunchedEffect(viewModel) {
        viewModel
            .hasChecked
            .filter { it }
            .take(1)
            .collect {
                onReady()
            }
    }

    val requestPermission = rememberRequestPermission(viewModel)

    RationaleDialog(viewModel)

    val context = LocalContext.current
    val activity = context as? ComponentActivity ?: return

    NavigationEvent(viewModel) { e ->
        when (e) {
            PermissionViewModel.Event.PermissionDenied -> {
                // ユーザーによって必要な権限が拒否されたらアプリを終了
                Toast.makeText(
                    context,
                    context.getString(R.string.message_permission_denied),
                    Toast.LENGTH_SHORT,
                ).show()
            }

            is PermissionViewModel.Event.RequestPermission -> {
                requestPermission(e.rationale)
            }

            is PermissionViewModel.Event.MissingRequirement.LocationPermission -> {
                val rationale = PermissionRationale.LocationPermission(
                    showSystemRequestDialog = e.state.canShowSystemRequestDialog(activity),
                )
                if (e.state.shouldShowRationale) {
                    // 必要なら権限リクエストを説明する
                    viewModel.showRationale(rationale)
                } else {
                    viewModel.requestPermission(rationale)
                }
            }

            is PermissionViewModel.Event.MissingRequirement.NotificationPermission -> {
                val rationale = PermissionRationale.NotificationPermission(
                    showSystemRequestDialog = e.state.canShowSystemRequestDialog(activity),
                )
                if (e.state.shouldShowRationale) {
                    // 必要なら権限リクエストを説明する
                    viewModel.showRationale(rationale)
                } else {
                    viewModel.requestPermission(rationale)
                }
            }

            is PermissionViewModel.Event.MissingRequirement.GooglePlayService -> {
                // 特に説明は不要
                GoogleApiAvailability
                    .getInstance()
                    .getErrorDialog(activity, e.errorCode, 0)
                    ?.show()
            }

            PermissionViewModel.Event.MissingRequirement.DrawOverlay -> {
                // 権限リクエストを説明する
                viewModel.showRationale(PermissionRationale.DrawOverlay)
            }

            PermissionViewModel.Event.MissingRequirement.NotificationChannel -> {
                // 権限リクエストを説明する
                viewModel.showRationale(PermissionRationale.NotificationChannel)
            }
        }
    }
}

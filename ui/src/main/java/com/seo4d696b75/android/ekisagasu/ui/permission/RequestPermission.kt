package com.seo4d696b75.android.ekisagasu.ui.permission

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

@Composable
fun rememberRequestPermission(
    viewModel: PermissionViewModel,
): (PermissionRationale) -> Unit {
    val context = LocalContext.current

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = viewModel::onLocationPermissionResult,
    )

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = viewModel::onNotificationPermissionResult,
    )

    val overlayPermissionLauncher =
        rememberLauncherForActivityResult(contract = ActivityResultContracts.StartActivityForResult()) {
            // 常に Activity.RESULT_CANCELLED が返される
        }

    return remember(context) {
        { rationale: PermissionRationale ->
            when (rationale) {
                is PermissionRationale.LocationPermission -> {
                    if (rationale.showSystemRequestDialog) {
                        locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                    } else {
                        // 複数回拒否するとシステムの権限ダイアログを表示できないため設定画面に誘導する
                        val intent = Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.parse("package:${context.packageName}"),
                        )
                        context.startActivity(intent)
                    }
                }

                is PermissionRationale.NotificationPermission -> {
                    if (rationale.showSystemRequestDialog) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        // 複数回拒否するとシステムの権限ダイアログを表示できないため設定画面に誘導する
                        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                        }
                        context.startActivity(intent)
                    }
                }

                PermissionRationale.NotificationChannel -> {
                    // 通知チャネルは設定画面でのみ変更できる
                    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                        putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                    }
                    context.startActivity(intent)
                }

                PermissionRationale.DrawOverlay -> {
                    // 設定画面に遷移
                    val intent = Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:${context.packageName}"),
                    )
                    overlayPermissionLauncher.launch(intent)
                }
            }
        }
    }
}

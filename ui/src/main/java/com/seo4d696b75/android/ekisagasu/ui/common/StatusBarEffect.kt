package com.seo4d696b75.android.ekisagasu.ui.common

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

@Composable
fun StatusBarEffect(
    darkIcons: Boolean = !isSystemInDarkTheme(),
) {
    val view = LocalView.current
    val activity = view.context as? Activity ?: return
    val window = activity.window

    val windowInsetsController = remember(view, window) {
        WindowCompat.getInsetsController(window, view)
    }

    LaunchedEffect(windowInsetsController, darkIcons) {
        windowInsetsController.isAppearanceLightStatusBars = darkIcons
    }
}

package com.seo4d696b75.android.ekisagasu.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import com.seo4d696b75.android.ekisagasu.ui.R
import kotlinx.serialization.Serializable

/** BottomNavigationのタブを定義 */
sealed interface NavigationTab {
    @get:Composable
    val icon: ImageVector

    val label: String

    companion object {
        val entries = listOf(Home, Log, Setting)
    }

    @Serializable
    data object Home : NavigationTab {
        @get:Composable
        override val icon
            get() = Icons.Outlined.Home

        override val label = "ホーム"
    }

    @Serializable
    data object Log : NavigationTab {
        override val icon
            @Composable
            get() = ImageVector.vectorResource(id = R.drawable.ic_history)

        override val label = "ログ"
    }

    @Serializable
    data object Setting : NavigationTab {
        @get:Composable
        override val icon
            get() = Icons.Outlined.Settings

        override val label = "設定"
    }
}

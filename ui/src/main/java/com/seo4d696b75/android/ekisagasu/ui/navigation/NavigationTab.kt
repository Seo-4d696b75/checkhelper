package com.seo4d696b75.android.ekisagasu.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import com.seo4d696b75.android.ekisagasu.ui.R
import kotlinx.serialization.Serializable

/** BottomNavigationのタブを定義 */
sealed interface NavigationTab {
    @Composable
    fun icon(selected: Boolean): ImageVector

    @Composable
    fun label(): String

    companion object {
        val entries = listOf(Home, Log, Setting)
    }

    @Serializable
    data object Home : NavigationTab {
        @Composable
        override fun icon(selected: Boolean) = if (selected) {
            Icons.Filled.Home
        } else {
            Icons.Outlined.Home
        }

        @Composable
        override fun label() = stringResource(id = R.string.navigation_bar_label_home)
    }

    @Serializable
    data object Log : NavigationTab {
        @Composable
        override fun icon(selected: Boolean) = ImageVector.vectorResource(
            id = if (selected) {
                R.drawable.ic_database_filled
            } else {
                R.drawable.ic_database_outlined
            }
        )

        @Composable
        override fun label() = stringResource(id = R.string.navigation_bar_label_log)
    }

    @Serializable
    data object Setting : NavigationTab {
        @Composable
        override fun icon(selected: Boolean) = if (selected) {
            Icons.Filled.Settings
        } else {
            Icons.Outlined.Settings
        }

        @Composable
        override fun label() = stringResource(id = R.string.navigation_bar_label_setting)
    }
}

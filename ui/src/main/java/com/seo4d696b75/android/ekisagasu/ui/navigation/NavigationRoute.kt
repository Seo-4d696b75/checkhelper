package com.seo4d696b75.android.ekisagasu.ui.navigation

import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.toRoute
import com.seo4d696b75.android.ekisagasu.domain.dataset.LatestDataVersion
import com.seo4d696b75.android.ekisagasu.domain.dataset.update.DataUpdateType
import kotlinx.serialization.Serializable

sealed interface NavigationRoute {
    /** BottomNavigationを非表示にする画面 */
    interface Modal

    sealed interface Home : NavigationRoute {
        @Serializable
        data object Radar : Home

        @Serializable
        data class Station(val code: Int) : Home

        @Serializable
        data class Line(val code: Int) : Home
    }

    sealed interface Log : NavigationRoute {
        @Serializable
        data object Top : Log
    }

    sealed interface Setting : NavigationRoute {
        @Serializable
        data object Top : Setting
    }

    sealed interface DataUpdate : NavigationRoute {
        @Serializable
        data class ConfirmDialog(
            val type: DataUpdateType,
            val info: LatestDataVersion,
        ) : DataUpdate

        @Serializable
        data class ExecuteDialog(
            val type: DataUpdateType,
            val info: LatestDataVersion,
        ) : DataUpdate

        @Serializable
        data object SuccessDialog : DataUpdate

        @Serializable
        data class RetryDialog(
            val type: DataUpdateType,
            val info: LatestDataVersion,
        ) : DataUpdate
    }
}

fun NavBackStackEntry.toRoute(): NavigationRoute = when {
    destination.hasRoute<NavigationRoute.Home.Radar>() -> NavigationRoute.Home.Radar
    destination.hasRoute<NavigationRoute.Home.Station>() -> toRoute<NavigationRoute.Home.Station>()
    destination.hasRoute<NavigationRoute.Home.Line>() -> toRoute<NavigationRoute.Home.Line>()
    destination.hasRoute<NavigationRoute.Log.Top>() -> NavigationRoute.Log.Top
    destination.hasRoute<NavigationRoute.Setting.Top>() -> NavigationRoute.Setting.Top
    destination.hasRoute<NavigationRoute.DataUpdate.ConfirmDialog>() -> toRoute<NavigationRoute.DataUpdate.ConfirmDialog>()
    destination.hasRoute<NavigationRoute.DataUpdate.ExecuteDialog>() -> toRoute<NavigationRoute.DataUpdate.ExecuteDialog>()
    destination.hasRoute<NavigationRoute.DataUpdate.SuccessDialog>() -> NavigationRoute.DataUpdate.SuccessDialog
    destination.hasRoute<NavigationRoute.DataUpdate.RetryDialog>() -> toRoute<NavigationRoute.DataUpdate.RetryDialog>()
    else -> throw IllegalStateException("unexpected route destination: $destination")
}

fun NavigationRoute.toTab(): NavigationTab? = when (this) {
    is NavigationRoute.Home -> NavigationTab.Home
    is NavigationRoute.Log -> NavigationTab.Log
    is NavigationRoute.Setting -> NavigationTab.Setting
    else -> null
}

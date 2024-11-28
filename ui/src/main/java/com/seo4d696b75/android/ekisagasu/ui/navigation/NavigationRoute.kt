package com.seo4d696b75.android.ekisagasu.ui.navigation

import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination.Companion.hasRoute
import kotlinx.serialization.Serializable

sealed interface NavigationRoute {
    /** BottomNavigationを非表示にする画面 */
    interface Modal

    sealed interface Home : NavigationRoute {
        @Serializable
        data object Top : Home
    }

    sealed interface Log : NavigationRoute {
        @Serializable
        data object Top : Log
    }

    sealed interface Setting : NavigationRoute {
        @Serializable
        data object Top : Setting
    }
}

fun NavBackStackEntry.toRoute(): NavigationRoute = when {
    destination.hasRoute<NavigationRoute.Home.Top>() -> NavigationRoute.Home.Top
    destination.hasRoute<NavigationRoute.Log.Top>() -> NavigationRoute.Log.Top
    destination.hasRoute<NavigationRoute.Setting.Top>() -> NavigationRoute.Setting.Top
    else -> throw IllegalStateException("unexpected route destination: $destination")
}

fun NavigationRoute.findTab(): NavigationTab = when (this) {
    is NavigationRoute.Home -> NavigationTab.Home
    is NavigationRoute.Log -> NavigationTab.Log
    is NavigationRoute.Setting -> NavigationTab.Setting
}

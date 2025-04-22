package com.seo4d696b75.android.ekisagasu.ui.navigator

data class NavigatorUiState(
    val visible: Boolean,
    val isExpanded: Boolean,
    val navigation: DisplayedNavigatorState,
) {
    companion object {
        val Initial = NavigatorUiState(
            visible = false,
            isExpanded = true,
            navigation = DisplayedNavigatorState.Idle,
        )
    }
}

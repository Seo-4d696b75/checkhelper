package com.seo4d696b75.android.ekisagasu.ui.navigator

import android.view.View
import android.widget.TextView
import androidx.databinding.BindingAdapter

@BindingAdapter("visibleForInitializing")
fun setVisibleForInitializing(
    view: View,
    state: NavigatorUiState?,
) {
    view.visibility = when (state) {
        is NavigatorUiState.Initializing -> View.VISIBLE
        else -> View.GONE
    }
}

@BindingAdapter("navigatorLine")
fun setNavigatorLineName(
    view: TextView,
    state: NavigatorUiState?,
) {
    view.text = when (state) {
        is NavigatorUiState.Initializing -> state.line.name
        is NavigatorUiState.Result -> state.line.name
        else -> null
    }
}

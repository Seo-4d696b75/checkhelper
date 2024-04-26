package com.seo4d696b75.android.ekisagasu.ui.navigator

import android.view.View
import android.widget.TextView
import androidx.core.text.buildSpannedString
import androidx.core.text.color
import androidx.databinding.BindingAdapter
import com.seo4d696b75.android.ekisagasu.ui.R
import com.seo4d696b75.android.ekisagasu.ui.utils.formatDistance

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

@BindingAdapter("visibleForNavigator")
fun setVisibleForNavigator(
    view: View,
    state: NavigatorUiState?,
) {
    view.visibility = when (state) {
        is NavigatorUiState.Result -> View.VISIBLE
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

@BindingAdapter("navigatorStationDistance")
fun setNavigatorStationDistance(
    view: TextView,
    state: NavigatorStationUiState?,
) {
    view.text = when (state) {
        is NavigatorStationUiState.Prediction -> state.distance.formatDistance
        else -> ""
    }
}

@BindingAdapter("navigatorLines")
fun setNavigatorLines(
    view: TextView,
    state: NavigatorStationUiState?,
) {
    view.text = if (state == null) {
        ""
    } else {
        val sorted = state.lines.sortedBy {
            if (it.isCurrentSelected) 0 else 1
        }
        buildSpannedString {
            val iterator = sorted.iterator()
            while (iterator.hasNext()) {
                val line = iterator.next()
                val rgb = view.context.getColor(
                    if (line.isCurrentSelected) R.color.colorPrimaryDark else R.color.colorTextGray
                )
                color(rgb) {
                    append(line.line.name)
                }
                if (iterator.hasNext()) {
                    append(" ")
                }
            }
        }
    }
}

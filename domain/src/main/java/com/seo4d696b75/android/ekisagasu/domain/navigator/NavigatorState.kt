package com.seo4d696b75.android.ekisagasu.domain.navigator

import com.seo4d696b75.android.ekisagasu.domain.dataset.Line
import com.seo4d696b75.android.ekisagasu.domain.dataset.Station

sealed interface NavigatorState {
    val line: Line

    data class Initializing(
        override val line: Line,
    ) : NavigatorState

    data class Result(
        override val line: Line,
        val current: Station,
        val predictions: List<NavigatorPrediction>,
    ) : NavigatorState
}

data class NavigatorPrediction(
    val station: Station,
    val distance: Float,
)

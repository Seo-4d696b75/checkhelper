package com.seo4d696b75.android.ekisagasu.domain.navigator

import com.seo4d696b75.android.ekisagasu.domain.dataset.Station

class StationPrediction(val station: Station, var distance: Float,) : Comparable<StationPrediction> {
    override operator fun compareTo(other: StationPrediction): Int = distance.compareTo(other.distance)

    fun compareDistance(other: StationPrediction) {
        distance = distance.coerceAtMost(other.distance)
    }
}

class PredictionResult(val size: Int, val current: Station,) {
    val predictions: Array<StationPrediction?> = arrayOfNulls(size)

    fun getStation(index: Int): Station = predictions[index]?.station ?: throw IllegalStateException("not init yet")

    fun getDistance(index: Int): Float = predictions[index]?.distance ?: throw IllegalStateException("not init yet")
}

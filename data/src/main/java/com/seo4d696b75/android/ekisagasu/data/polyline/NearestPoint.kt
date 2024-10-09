package com.seo4d696b75.android.ekisagasu.data.polyline

import com.google.android.gms.maps.model.LatLng
import com.seo4d696b75.android.ekisagasu.data.kdtree.measureDistance
import kotlin.math.pow

data class NearestPoint(
    val start: LatLng,
    val end: LatLng,
    val index: Float,
    val nearest: LatLng,
    val distance: Float,
    val edgeDistance: Float,
) {

    fun distanceFrom(): Float = edgeDistance * index

    fun distanceTo(): Float = edgeDistance * (1 - index)

    companion object {
        fun from(start: LatLng, end: LatLng, point: LatLng): NearestPoint {
            val v1 =
                (point.longitude - start.longitude) * (end.longitude - start.longitude) + (point.latitude - start.latitude) * (end.latitude - start.latitude)
            val v2 =
                (point.longitude - end.longitude) * (start.longitude - end.longitude) + (point.latitude - end.latitude) * (start.latitude - end.latitude)
            val index = if (v1 >= 0 && v2 >= 0) {
                val squared = (start.longitude - end.longitude).pow(2.0) + (start.latitude - end.latitude).pow(2.0)
                (v1 / squared).toFloat()
            } else if (v1 < 0) {
                0f
            } else {
                1f
            }
            require(index in 0f..1f)
            val lon = (1 - index) * start.longitude + index * end.longitude
            val lat = (1 - index) * start.latitude + index * end.latitude
            val nearest = LatLng(lat, lon)
            val distance = nearest.measureDistance(point)
            val edgeDistance = start.measureDistance(end)
            return NearestPoint(start, end, index, nearest, distance, edgeDistance)
        }
    }
}

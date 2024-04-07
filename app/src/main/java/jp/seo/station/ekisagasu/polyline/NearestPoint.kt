package jp.seo.station.ekisagasu.polyline

import com.google.android.gms.maps.model.LatLng
import jp.seo.station.ekisagasu.search.measureDistance
import java.util.Locale

class NearestPoint(
    val start: LatLng,
    val end: LatLng,
    point: LatLng,
) {
    var index = 0.0
    val distance: Float
    val edgeDistance: Float
    val closedPoint: LatLng
    var isOnEdge = false

    fun distanceFrom(): Float {
        return edgeDistance * index.toFloat()
    }

    fun distanceTo(): Float {
        return edgeDistance * (1 - index.toFloat())
    }

    override fun toString(): String {
        return String.format(
            Locale.US,
            "NearestPoint(lat/lon:(%.6f,%.6f) - %.2fm)",
            closedPoint.latitude,
            closedPoint.longitude,
            distance,
        )
    }

    init {
        val v1 =
            (point.longitude - start.longitude) * (end.longitude - start.longitude) +
                (point.latitude - start.latitude) * (end.latitude - start.latitude)
        val v2 =
            (point.longitude - end.longitude) * (start.longitude - end.longitude) +
                (point.latitude - end.latitude) * (start.latitude - end.latitude)
        if (v1 >= 0 && v2 >= 0) {
            isOnEdge = true
            index = v1 /
                Math.pow(start.longitude - end.longitude, 2.0) + Math.pow(start.latitude - end.latitude, 2.0)
        } else if (v1 < 0) {
            isOnEdge = false
            index = 0.0
        } else {
            isOnEdge = false
            index = 1.0
        }
        val lon = (1 - index) * start.longitude + index * end.longitude
        val lat = (1 - index) * start.latitude + index * end.latitude
        closedPoint = LatLng(lat, lon)
        distance = closedPoint.measureDistance(point)
        edgeDistance = start.measureDistance(end)
    }
}

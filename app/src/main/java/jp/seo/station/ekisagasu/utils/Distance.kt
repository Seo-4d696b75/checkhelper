package jp.seo.station.ekisagasu.utils

import android.location.Location
import jp.seo.station.ekisagasu.Station
import kotlin.math.absoluteValue
import kotlin.math.cos
import kotlin.math.sqrt

/**
 * @author Seo-4d696b75
 * @version 2021/01/07.
 */
class Distance {
}

fun measureDistance(station: Station, location: Location): Double {
    return measureDistance(station, location.latitude, location.longitude)
}

fun measureDistance(station: Station, lat: Double, lng: Double): Double {
    return measureDistance(station.lat, station.lng, lat, lng)
}

fun measureDistance(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
    val r = 6378137.0
    val x1 = Math.toRadians(lng1)
    val y1 = Math.toRadians(lat1)
    val x2 = Math.toRadians(lng2)
    val y2 = Math.toRadians(lat2)
    val lat = r * (y1 - y2).absoluteValue
    val lng = r * cos((y1 + y2) / 2) * (x1 - x2).absoluteValue
    return sqrt(lat * lat + lng * lng)
}

fun formatDistance(dist: Double): String {
    return if (dist < 1000.0) {
        String.format("%.0fm", dist)
    } else {
        String.format("%.2fkm", dist / 1000.0)
    }
}

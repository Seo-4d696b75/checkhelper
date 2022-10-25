package jp.seo.station.ekisagasu.search

import android.location.Location
import com.google.android.gms.maps.model.LatLng
import jp.seo.android.diagram.Point
import jp.seo.station.ekisagasu.model.Station
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * @author Seo-4d696b75
 * @version 2021/01/07.
 */

fun measureDistance(station: Station, location: Location): Double {
    return measureDistance(station, location.latitude, location.longitude)
}

fun measureDistance(station: Station, lat: Double, lng: Double): Double {
    return measureDistance(station.lat, station.lng, lat, lng)
}

const val SPHERE_RADIUS = 6378137.0

fun measureDistance(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {

    val x1 = Math.toRadians(lng1)
    val y1 = Math.toRadians(lat1)
    val x2 = Math.toRadians(lng2)
    val y2 = Math.toRadians(lat2)

    val x = (x1 - x2) / 2
    val y = (y1 - y2) / 2
    return SPHERE_RADIUS * 2 * asin(
        sqrt(
            sin(y).pow(2) + cos(y1) * cos(y2) * sin(x).pow(2)
        )
    )
}

fun formatDistance(dist: Double): String {
    return if (dist < 1000.0) {
        String.format("%.0fm", dist)
    } else if (dist < 10000.0) {
        String.format("%.2fkm", dist / 1000.0)
    } else if (dist < 100000.0) {
        String.format("%.1fkm", dist / 1000.0)
    } else {
        String.format("%.0fkm", dist / 1000.0)
    }
}

fun measure(
    lat1: Double,
    lng1: Double,
    lat2: Double,
    lng2: Double,
    sphere: Boolean,
): Double {
    return if (sphere) {
        measureDistance(lat1, lng1, lat2, lng2)
    } else {
        sqrt((lat1 - lat2).pow(2) + (lng1 - lng2).pow(2))
    }
}

fun LatLng.measureEuclid(other: LatLng): Double {
    return measure(latitude, longitude, other.latitude, other.longitude, false)
}

fun LatLng.measureEuclid(other: Point): Double {
    return measure(latitude, longitude, other.y, other.x, false)
}

fun LatLng.measureDistance(other: Point): Double {
    return measure(latitude, longitude, other.y, other.x, true)
}

fun LatLng.measureDistance(other: LatLng): Double {
    return measure(latitude, longitude, other.latitude, other.longitude, true)
}

package com.seo4d696b75.android.ekisagasu.data.kdtree

import com.google.android.gms.maps.model.LatLng
import com.seo4d696b75.android.ekisagasu.domain.dataset.Station
import com.seo4d696b75.android.ekisagasu.domain.location.Location
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * @author Seo-4d696b75
 * @version 2022/10/25
 */

const val SPHERE_RADIUS = 6378137.0

/**
 * 地球を完全な球体モデルで測地線距離で測定 メートル単位
 */
fun measureSphere(
    lat1: Double,
    lng1: Double,
    lat2: Double,
    lng2: Double,
): Double {
    val x1 = Math.toRadians(lng1)
    val y1 = Math.toRadians(lat1)
    val x2 = Math.toRadians(lng2)
    val y2 = Math.toRadians(lat2)

    val x = (x1 - x2) / 2
    val y = (y1 - y2) / 2
    return SPHERE_RADIUS * 2 *
        asin(
            sqrt(
                sin(y).pow(2) + cos(y1) * cos(y2) * sin(x).pow(2),
            ),
        )
}

/**
 * 緯度・経度を直交座標系に見なしてユークリッド距離を計算
 */
fun measureEuclid(
    lat1: Double,
    lng1: Double,
    lat2: Double,
    lng2: Double,
) = sqrt(
    (lat1 - lat2).pow(2) + (lng1 - lng2).pow(2),
)

/**
 * WGS84 測地系で距離を計算 メートル単位
 */
fun measureDistance(
    lat1: Double,
    lng1: Double,
    lat2: Double,
    lng2: Double,
) = FloatArray(1).let {
    android.location.Location.distanceBetween(lat1, lng1, lat2, lng2, it)
    it[0]
}

fun Location.measureDistance(
    other: Location,
) = measureDistance(lat, lng, other.lat, other.lng)

fun measure(
    lat1: Double,
    lng1: Double,
    lat2: Double,
    lng2: Double,
    sphere: Boolean,
): Double = if (sphere) {
    measureSphere(lat1, lng1, lat2, lng2)
} else {
    measureEuclid(lat1, lng1, lat2, lng2)
}

fun LatLng.measureEuclid(other: LatLng) =
    measureEuclid(
        latitude,
        longitude,
        other.latitude,
        other.longitude,
    )

fun LatLng.measureDistance(other: LatLng) =
    measureDistance(
        latitude,
        longitude,
        other.latitude,
        other.longitude,
    )

fun Station.measureDistance(lat: Double, lng: Double) =
    measureDistance(
        this.lat,
        this.lng,
        lat,
        lng,
    )

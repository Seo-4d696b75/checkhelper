package com.seo4d696b75.android.ekisagasu.data.polyline

import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.data.geojson.GeoJsonFeature
import com.google.maps.android.data.geojson.GeoJsonLineString
import com.google.maps.android.data.geojson.GeoJsonParser
import com.google.maps.android.data.geojson.GeoJsonPolygon
import com.seo4d696b75.android.ekisagasu.domain.dataset.Station
import org.json.JSONObject

/**
 * @author Seo-4d696b75
 * @version 2020/12/17.
 */

/**
 * String -> JSONObject -> GeoJsonFeature
 * "type" must be "Feature".
 * @link https://geojson.org/geojson-spec.html
 */
fun convertGeoJsonFeature(value: String): GeoJsonFeature {
    val obj = JSONObject(value)
    val parser = GeoJsonParser(obj)
    return parser.features[0]
}

fun convertGeoJsonFeatureCollections(value: String): List<GeoJsonFeature> {
    val obj = JSONObject(value)
    val parser = GeoJsonParser(obj)
    return parser.features
}

data class StationArea(val station: Station, val points: Array<LatLng>, val enclosed: Boolean,) {
    companion object {
        fun parseArea(station: Station): StationArea {
            val feature = convertGeoJsonFeature(station.voronoi)
            return when (feature.geometry.geometryType) {
                "LineString" -> {
                    val geo = feature.geometry as GeoJsonLineString
                    StationArea(
                        station,
                        geo.coordinates.toTypedArray(),
                        false,
                    )
                }

                "Polygon" -> {
                    val geo = feature.geometry as GeoJsonPolygon
                    val list = geo.coordinates[0]
                    list.removeLast()
                    StationArea(
                        station,
                        list.toTypedArray(),
                        true,
                    )
                }

                else -> throw IllegalArgumentException("invalid geometry type")
            }
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as StationArea

        if (this.station.code != other.station.code) return false

        return true
    }

    override fun hashCode(): Int {
        return station.hashCode()
    }
}

data class PolylineSegment(val points: Array<LatLng>, val start: String, val end: String,) {
    companion object {
        fun parseSegments(data: String): List<PolylineSegment> {
            return convertGeoJsonFeatureCollections(data).map {
                if (it.geometry.geometryType != "LineString") {
                    throw RuntimeException("not LineString. type:" + it.geometry.geometryType)
                }
                val geo = it.geometry as GeoJsonLineString
                val points = geo.coordinates.toTypedArray()
                PolylineSegment(
                    points,
                    it.getProperty("start"),
                    it.getProperty("end"),
                )
            }
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PolylineSegment

        if (start != other.start) return false
        if (end != other.end) return false
        if (this.points.size != other.points.size) return false
        val idx = this.points.size / 2
        if (this.points[idx] != other.points[idx]) return false

        return true
    }

    override fun hashCode(): Int {
        var result = start.hashCode()
        result = 31 * result + end.hashCode()
        result = 31 * result + points.size
        result = 31 * result + points[points.size / 2].hashCode()
        return result
    }

    fun findNearestPoint(
        lat: Double,
        lng: Double,
    ): NearestPoint {
        val step = 50
        val p = LatLng(lat, lng)
        var minValue = Double.MAX_VALUE
        var minStart = -1
        var minEnd = -1
        run {
            var start = 0
            while (start < points.size) {
                var end = start + step
                if (end >= points.size) end = points.size - 1
                if (start == end) break
                val n = NearestPoint(points[start], points[end], p)
                if (n.distance < minValue) {
                    minValue = n.distance.toDouble()
                    minStart = start
                    minEnd = end
                }
                start += step
            }
        }
        return (minStart until minEnd)
            .map { idx -> NearestPoint(points[idx], points[idx + 1], p) }
            .minByOrNull { n -> n.distance } ?: throw NoSuchElementException()
    }
}

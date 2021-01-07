package jp.seo.station.ekisagasu.utils

import android.util.Log
import androidx.room.TypeConverter
import com.google.maps.android.data.geojson.GeoJsonFeature
import com.google.maps.android.data.geojson.GeoJsonParser
import org.json.JSONObject
import java.lang.Exception
import java.lang.RuntimeException

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

data class PolylineSegment(
    val feature: GeoJsonFeature,
    val start: String,
    val end: String
) {

    companion object {

        fun parseSegments(data: String): List<PolylineSegment> {
            return convertGeoJsonFeatureCollections(data).map {
                if (it.geometry.geometryType != "LineString") throw RuntimeException("not LineString. type:" + it.geometry.geometryType)
                PolylineSegment(
                    it,
                    it.getProperty("start"),
                    it.getProperty("end")
                )
            }
        }

    }

}

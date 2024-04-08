package jp.seo.station.ekisagasu.polyline

import com.google.android.gms.maps.model.LatLng
import jp.seo.diagram.core.BasePoint
import jp.seo.diagram.core.Edge
import jp.seo.diagram.core.Point
import jp.seo.station.ekisagasu.BuildConfig
import jp.seo.station.ekisagasu.model.StationArea
import jp.seo.station.ekisagasu.search.NearestSearch

/**
 * 境界線をなす各辺に繰り返し処理を実行
 */
suspend fun StationArea.forEachEdge(process: suspend ((a: LatLng, b: LatLng) -> Unit)) {
    var a = points[if (enclosed) points.size - 1 else 0]
    var i = if (enclosed) 0 else 1

    while (i < points.size) {
        val b = points[i]
        process(a, b)
        a = b
        i++
    }
}

suspend fun StationArea.getIntersection(edge: Edge): Point? {
    var found: Point? = null
    forEachEdge { a, b ->
        val e = Edge(a.point2D, b.point2D)
        e.getIntersection(edge)?.let {
            found = it
            return@forEachEdge
        }
    }
    return found
}

val LatLng.point2D: Point
    get() = BasePoint(longitude, latitude)

val Point.latLng: LatLng
    get() = LatLng(y, x)

suspend fun NearestSearch.searchEuclid(pos: LatLng) =
    search(
        pos.latitude,
        pos.longitude,
        1,
        0.0,
        false,
    ).stations.firstOrNull()

suspend inline fun assert(check: suspend () -> Boolean) {
    if (BuildConfig.DEBUG && !check()) {
        throw AssertionError()
    }
}

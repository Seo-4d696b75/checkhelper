package jp.seo.station.ekisagasu.position

import com.google.android.gms.maps.model.LatLng
import jp.seo.station.ekisagasu.model.StationArea

/**
 * 境界線をなす各辺に繰り返し処理を実行
 */
suspend fun StationArea.forEachEdge(
    process: suspend ((a: LatLng, b: LatLng) -> Unit),
) {
    var a = points[if (enclosed) points.size - 1 else 0]
    var i = if (enclosed) 0 else 1

    while (i < points.size) {
        val b = points[i]
        process(a, b)
        a = b
        i++
    }
}

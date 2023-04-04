package jp.seo.station.ekisagasu.polyline

import android.location.Location
import android.os.SystemClock
import jp.seo.station.ekisagasu.model.Line
import jp.seo.station.ekisagasu.model.PolylineSegment
import jp.seo.station.ekisagasu.model.Station
import jp.seo.station.ekisagasu.model.StationArea
import jp.seo.station.ekisagasu.search.NearestSearch
import jp.seo.station.ekisagasu.utils.TIME_PATTERN_MILLI_SEC
import jp.seo.station.ekisagasu.utils.formatTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.Date

/**
 * @author Seo-4d696b75
 * @version 2019/02/16.
 */

class StationPrediction(
    val station: Station,
    var distance: Float
) :
    Comparable<StationPrediction> {
    override operator fun compareTo(other: StationPrediction): Int {
        return distance.compareTo(other.distance)
    }

    fun compareDistance(other: StationPrediction) {
        distance = distance.coerceAtMost(other.distance)
    }
}

class PredictionResult(
    val size: Int,
    val current: Station,
) {
    val predictions: Array<StationPrediction?> = arrayOfNulls(size)
    fun getStation(index: Int): Station {
        return predictions[index]?.station ?: throw IllegalStateException("not init yet")
    }

    fun getDistance(index: Int): Float {
        return predictions[index]?.distance ?: throw IllegalStateException("not init yet")
    }
}

class PolylineNavigator(
    private val explorer: NearestSearch,
    val line: Line
) {

    companion object {
        private const val DISTANCE_THRESHOLD = 5f
    }

    fun release() {
        for (p in cursors) p.release()
        prediction.clear()
        cursors.clear()
    }

    private val _results = MutableStateFlow<PredictionResult?>(null)
    val results: StateFlow<PredictionResult?> = _results

    private fun setPolylineFragment(tag: String, fragment: PolylineSegment) {
        val list = if (segmentJunction.containsKey(tag)) {
            segmentJunction[tag] ?: throw RuntimeException("expected not to be happened")
        } else {
            val list = mutableListOf<PolylineSegment>()
            segmentJunction[tag] = list
            list
        }
        list.add(fragment)
    }

    var maxPrediction: Int = 2

    private val segmentJunction = mutableMapOf<String, MutableList<PolylineSegment>>()
    private val polylineSegments = mutableListOf<PolylineSegment>()
    private var cursors = mutableListOf<PolylineCursor>()
    private var currentStation: StationArea? = null
    private var prediction = mutableListOf<StationPrediction>()
    private var lastLocation: Location? = null

    private var updateTime: Long = 0

    init {
        if (line.polyline == null) {
            throw IllegalArgumentException("Polyline not set, but prediction required. line:" + line.name)
        }
        PolylineSegment.parseSegments(line.polyline).forEach { segment ->
            setPolylineFragment(segment.start, segment)
            setPolylineFragment(segment.end, segment)
            polylineSegments.add(segment)
        }
    }

    private val lock = Mutex()

    suspend fun onLocationUpdate(location: Location, station: Station) =
        withContext(Dispatchers.IO) {
            if (!location.latitude.isFinite() || !location.longitude.isFinite()) return@withContext
            assert {
                location.latitude in -90.0..90.0
                        && location.longitude in -180.0..180.0
            }
            lock.withLock {
                val start = SystemClock.uptimeMillis()
                updateTime = location.elapsedRealtimeNanos / 1000000L
                if (cursors.isEmpty()) {
                    initialize(location)
                    val result = PredictionResult(0, station)
                    _results.value = result
                    return@withContext
                }
                if (currentStation?.station != station) {
                    currentStation = StationArea.parseArea(station)
                }
                // Update each cursors
                val list = mutableListOf<PolylineCursor>()
                for (p in cursors) p.update(location, list)

                // Filter cursors
                if (cursors.size > 1) filterCursors(list)
                cursors = list
                Timber.tag("Navigator").d("cursor size: %d", list.size)
                if ((lastLocation?.distanceTo(location) ?: 100000f) < DISTANCE_THRESHOLD) {
                    Timber.tag("Navigator").d("location diff too small, skipped")
                    return@withContext
                }
                lastLocation = location

                // prediction の集計
                val resolved: MutableList<StationPrediction> = mutableListOf()
                val predictions: MutableList<StationPrediction> = mutableListOf()
                for (p in list) {
                    predictions.clear()
                    p.predict(predictions, maxPrediction, currentStation)
                    // 駅の重複がないように、重複するならより近い距離を採用
                    for (prediction in predictions) {
                        val same = getSameStation(resolved, prediction.station)
                        same?.compareDistance(prediction) ?: resolved.add(prediction)
                    }
                }

                // 距離に関して駅をソート
                resolved.sort()
                prediction = resolved
                val size = maxPrediction.coerceAtMost(prediction.size)
                // 結果オブジェクトにまとめる
                val result = PredictionResult(size, station)
                val date: String = formatTime(TIME_PATTERN_MILLI_SEC, Date(updateTime))
                Timber.tag("Navigator").d("predict date: $date, station size: ${prediction.size}")
                for (i in 0 until size) {
                    val s = prediction[i]
                    result.predictions[i] = s
                    Timber.tag("Navigator").d(
                        "[%d] %.0fm %s",
                        i,
                        s.distance,
                        s.station.name,
                    )
                }
                _results.value = result

                val duration = SystemClock.uptimeMillis() - start
                Timber.tag("Navigator").d("update $duration [ms]")
            }
        }

    private fun initialize(location: Location) {
        polylineSegments.map { f ->
            Pair(
                f,
                f.findNearestPoint(location.latitude, location.longitude)
            )
        }.minByOrNull { p -> p.second.distance }?.let {
            cursors.add(
                PolylineCursor(
                    it.first,
                    { tag -> segmentJunction[tag] ?: throw NoSuchElementException() },
                    it.second,
                    explorer
                )
            )
        }
        lastLocation = location
    }

    private fun getSameStation(
        list: List<StationPrediction>,
        station: Station
    ): StationPrediction? {
        for (p in list) {
            if (p.station == station) return p
        }
        return null
    }

    private fun filterCursors(list: MutableList<PolylineCursor>): Double {
        val minDistance = list.minOf { cursor -> cursor.nearest.distance }.toDouble()
        val iterator = list.iterator()
        while (iterator.hasNext()) {
            if (iterator.next().nearest.distance > minDistance * 2) {
                iterator.remove()
            }
        }
        return minDistance
    }

}

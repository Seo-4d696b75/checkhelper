package com.seo4d696b75.android.ekisagasu.data.polyline

import android.os.SystemClock
import com.seo4d696b75.android.ekisagasu.data.kdtree.measureDistance
import com.seo4d696b75.android.ekisagasu.domain.dataset.Line
import com.seo4d696b75.android.ekisagasu.domain.dataset.Station
import com.seo4d696b75.android.ekisagasu.domain.date.TIME_PATTERN_MILLI_SEC
import com.seo4d696b75.android.ekisagasu.domain.date.format
import com.seo4d696b75.android.ekisagasu.domain.kdtree.NearestSearch
import com.seo4d696b75.android.ekisagasu.domain.location.Location
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.Date

/**
 * @author Seo-4d696b75
 * @version 2019/02/16.
 */
class PolylineNavigator(private val explorer: NearestSearch, val line: Line) {
    companion object {
        private const val DISTANCE_THRESHOLD = 5f
    }

    fun release() {
        for (p in cursors) p.release()
        prediction.clear()
        cursors.clear()
    }

    private var _result: PredictionResult? = null
    val result: PredictionResult?
        get() = _result

    private fun setPolylineFragment(
        tag: String,
        fragment: PolylineSegment,
    ) {
        val list =
            if (segmentJunction.containsKey(tag)) {
                segmentJunction[tag] ?: throw RuntimeException("expected not to be happened")
            } else {
                val list = mutableListOf<PolylineSegment>()
                segmentJunction[tag] = list
                list
            }
        list.add(fragment)
    }

    private var maxPrediction: Int = 2

    private val segmentJunction = mutableMapOf<String, MutableList<PolylineSegment>>()
    private val polylineSegments = mutableListOf<PolylineSegment>()
    private var cursors = mutableListOf<PolylineCursor>()
    private var currentStation: StationArea? = null
    private var prediction = mutableListOf<StationPrediction>()
    private var lastLocation: Location? = null

    private var updateTime: Long = 0

    init {
        val data = line.polyline
            ?: throw IllegalArgumentException("Polyline not set, but prediction required. line:" + line.name)
        PolylineSegment.parseSegments(data).forEach { segment ->
            setPolylineFragment(segment.start, segment)
            setPolylineFragment(segment.end, segment)
            polylineSegments.add(segment)
        }
    }

    private val lock = Mutex()

    suspend fun onLocationUpdate(
        location: Location,
        station: Station,
    ) = withContext(Dispatchers.IO) {
        if (!location.lat.isFinite() || !location.lng.isFinite()) return@withContext
        assert {
            location.lat in -90.0..90.0 &&
                location.lng in -180.0..180.0
        }
        lock.withLock {
            val start = SystemClock.uptimeMillis()
            updateTime = location.elapsedRealtimeMillis
            if (cursors.isEmpty()) {
                initialize(location)
                val result = PredictionResult(0, station)
                _result = result
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
            if ((lastLocation?.measureDistance(location) ?: 100000f) < DISTANCE_THRESHOLD) {
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
            val date: String = Date(updateTime).format(TIME_PATTERN_MILLI_SEC)
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
            _result = result

            val duration = SystemClock.uptimeMillis() - start
            Timber.tag("Navigator").d("update $duration [ms]")
        }
    }

    private fun initialize(location: Location) {
        polylineSegments.map { f ->
            Pair(
                f,
                f.findNearestPoint(location.lat, location.lng),
            )
        }.minByOrNull { p -> p.second.distance }?.let {
            cursors.add(
                PolylineCursor(
                    it.first,
                    { tag -> segmentJunction[tag] ?: throw NoSuchElementException() },
                    it.second,
                    explorer,
                ),
            )
        }
        lastLocation = location
    }

    private fun getSameStation(
        list: List<StationPrediction>,
        station: Station,
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

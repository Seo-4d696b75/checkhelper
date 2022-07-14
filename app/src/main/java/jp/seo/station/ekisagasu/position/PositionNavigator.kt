package jp.seo.station.ekisagasu.position

import android.location.Location
import android.util.Log
import com.google.android.gms.maps.model.LatLng
import jp.seo.android.diagram.BasePoint
import jp.seo.android.diagram.Edge
import jp.seo.station.ekisagasu.database.StationDao
import jp.seo.station.ekisagasu.model.Line
import jp.seo.station.ekisagasu.model.Station
import jp.seo.station.ekisagasu.position.KalmanFilter.Sample
import jp.seo.station.ekisagasu.search.NearestSearch
import jp.seo.station.ekisagasu.utils.PolylineSegment
import jp.seo.station.ekisagasu.utils.StationArea
import jp.seo.station.ekisagasu.utils.TIME_PATTERN_MILLI_SEC
import jp.seo.station.ekisagasu.utils.formatTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.*
import kotlin.math.abs
import kotlin.math.pow


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

class PositionNavigator(
    private val explorer: NearestSearch,
    private val dao: StationDao,
    val line: Line
) {


    companion object {
        private const val DISTANCE_THRESHOLD = 5f

        private fun measureDistance(p1: LatLng, p2: LatLng): Float {
            return measureDistance(p1.latitude, p1.longitude, p2.latitude, p2.longitude)
        }

        private fun measureDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
            val result = FloatArray(1)
            Location.distanceBetween(lat1, lon1, lat2, lon2, result)
            return result[0]
        }
    }

    fun release() {
        for (p in cursors) p.release()
        prediction.clear()
        cursors.clear()
    }

    private val _results = MutableStateFlow<PredictionResult?>(null)
    val results: StateFlow<PredictionResult?> = _results

    private fun setPolylineFragment(tag: String, fragment: PolylineSegment) {
        val list = if (fragmentJunction.containsKey(tag)) {
            fragmentJunction[tag] ?: throw RuntimeException("expected not to be happened")
        } else {
            val list = mutableListOf<PolylineSegment>()
            fragmentJunction[tag] = list
            list
        }
        list.add(fragment)
    }

    var maxPrediction: Int = 2

    private val fragmentJunction = mutableMapOf<String, MutableList<PolylineSegment>>()
    private val polylineFragments = mutableListOf<PolylineSegment>()
    private var cursors = mutableListOf<PolylineCursor>()
    private var currentStation: StationArea? = null
    private var prediction = mutableListOf<StationPrediction>()
    private var lastLocation: Location? = null
    private val estimation = KalmanFilter()
    private var updateTime: Long = 0

    init {
        if (line.polyline == null) {
            throw IllegalArgumentException("Polyline not set, but prediction required. line:" + line.name)
        }
        PolylineSegment.parseSegments(line.polyline).forEach { segment ->
            setPolylineFragment(segment.start, segment)
            setPolylineFragment(segment.end, segment)
            polylineFragments.add(segment)
        }
    }

    private val lock = Mutex()

    suspend fun onLocationUpdate(location: Location, station: Station) =
        withContext(Dispatchers.IO) {
            lock.withLock {
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
                if (cursors.size > 1) filterCursors(list, 2.0)
                cursors = list
                Log.d(
                    "update",
                    String.format(
                        "cursor size: %d, speed: %.0fkm/h",
                        list.size,
                        list[0].state.speed * 3.6
                    )
                )
                if ((lastLocation?.distanceTo(location)
                        ?: 100000f) < DISTANCE_THRESHOLD
                ) return@withContext
                lastLocation = location

                // prediction の集計
                val resolved: MutableList<StationPrediction> = mutableListOf()
                val predictions: MutableList<StationPrediction> = mutableListOf()
                for (p in list) {
                    predictions.clear()
                    p.predict(predictions)
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
                Log.d("predict", date + " station size: " + prediction.size)
                for (i in 0 until size) {
                    val s = prediction[i]
                    result.predictions[i] = s
                    Log.d(
                        "predict",
                        java.lang.String.format(
                            Locale.US,
                            "[%d] %.0fm %s",
                            i,
                            s.distance,
                            s.station.name
                        )
                    )
                }
                _results.value = result
            }
        }

    private fun initialize(location: Location) {
        polylineFragments.map { f ->
            Pair<PolylineSegment, NearestPoint>(
                f,
                f.findNearestPoint(location.latitude, location.longitude)
            )
        }.minByOrNull { p -> p.second.distance }?.let {
            cursors.add(PolylineCursor(it.first, it.second, location))
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


    private fun filterCursors(list: MutableList<PolylineCursor>, threshold: Double): Double {
        val minDistance = list.minOf { cursor -> cursor.nearest.distance }.toDouble()
        val iterator = list.iterator()
        while (iterator.hasNext()) {
            if (iterator.next().nearest.distance > minDistance * threshold) {
                iterator.remove()
            }
        }
        return minDistance
    }

    private inner class PolylineCursor {
        /**
         * 初期化
         */
        constructor(
            segment: PolylineSegment,
            nearest: NearestPoint,
            location: Location
        ) {
            this.nearest = nearest
            // initialize node-node graph and start end node
            EndNode(segment, this)
            state = Sample.initialize(
                updateTime,
                nearest.distanceFrom().toDouble(),
                if (location.hasSpeed()) location.speed.toDouble() else 0.0,
                location.accuracy.coerceAtLeast(nearest.distance).toDouble()
            )
            pathLengthSign = 1
            pathPosAtStart = 0.0
            pathPosAtNearest = nearest.distanceFrom().toDouble()
        }

        /**
         * 新しい最近傍点でカーソルを新規生成
         *
         * @param start
         * @param end
         * @param nearest      最近傍点 on edge start-end
         * @param pathPosition 符号付経路上の距離@startノード
         */
        private constructor(
            start: PolylineNode,
            end: PolylineNode,
            nearest: NearestPoint,
            pathPosition: Double,
            old: PolylineCursor
        ) {
            this.nearest = nearest
            this.start = start
            this.end = end
            require(!(start.point != nearest.start || end.point != nearest.end))
            pathPosAtStart = pathPosition
            pathLengthSign = old.pathLengthSign
            pathPosAtNearest = pathPosition + nearest.distanceFrom() * pathLengthSign
            state = old.state
            isSignDecided = old.isSignDecided
        }

        /**
         * 以前のカーソルから新しい現在位置に対し探索を開始するカーソルを得る
         *
         * @param old      以前のカーソル
         * @param forward  向き
         * @param location 現在位置
         */
        private constructor(old: PolylineCursor, forward: Boolean, location: Location) {
            if (forward) {
                start = old.start
                end = old.end
                pathLengthSign = old.pathLengthSign
                pathPosAtStart = old.pathPosAtStart
            } else {
                start = old.end
                end = old.start
                pathLengthSign = -1 * old.pathLengthSign
                pathPosAtStart =
                    old.pathPosAtStart + old.pathLengthSign * old.nearest.edgeDistance
            }
            state = old.state
            val position = LatLng(location.latitude, location.longitude)
            nearest = NearestPoint(start.point, end.point, position)
            isSignDecided = old.isSignDecided
            pathPosAtNearest = pathPosAtStart + nearest.distanceFrom() * pathLengthSign
        }

        var nearest: NearestPoint

        private var _start: PolylineNode? = null
        private var _end: PolylineNode? = null

        var start: PolylineNode
            get() = _start ?: throw IllegalStateException("start node not set yet")
            set(value) {
                _start = value
            }
        var end: PolylineNode
            get() = _end ?: throw IllegalStateException("end node not set yet")
            set(value) {
                _end = value
            }

        // start -> end 方向を正方向とし、startを原点とする1次元座標で測定する
        var state: Sample
        var pathLengthSign = 0
        var pathPosAtStart = 0.0
        var pathPosAtNearest: Double
        var isSignDecided = false

        fun initPosition(from: PolylineNode, to: PolylineNode) {
            start = from
            end = to
        }

        fun update(location: Location, callback: MutableCollection<PolylineCursor>) {

            // 1. Search for the nearest point on this polyline to the given location in all the directions
            val list1 = mutableListOf<PolylineCursor>()
            val list2 = mutableListOf<PolylineCursor>()
            val v1 = searchForNearest(
                location,
                start,
                end,
                PolylineCursor(this, true, location),
                list1,
                pathPosAtStart + nearest.edgeDistance * pathLengthSign
            )
            val v2 = searchForNearest(
                location, end, start, PolylineCursor(this, false, location), list2,
                pathPosAtStart
            )
            val found: List<PolylineCursor> = if (v1 <= v2) list1 else list2

            // 2. Estimate current position using Kalman filter
            //    so that high-freq. noise should be removed
            for (cursor in found) {
                val next = estimation.update(
                    cursor.pathPosAtNearest,
                    cursor.nearest.distance.coerceAtLeast(location.accuracy).toDouble(),
                    updateTime, cursor.state
                )
                // 3. Check estimated position
                var reverse = next.speed * cursor.pathLengthSign < 0
                if (cursor.isSignDecided && abs(next.speed) < 5) {
                    // 一度進み始めた方向はそう変化しないはず
                    reverse = cursor.pathLengthSign * pathLengthSign < 0
                }
                if (!cursor.isSignDecided && abs(next.speed) > 5) {
                    cursor.isSignDecided = true
                }
                if (reverse) {
                    // direction reversed
                    val tmp = cursor.start
                    cursor.start = cursor.end
                    cursor.end = tmp
                    cursor.pathLengthSign *= -1
                    cursor.nearest =
                        NearestPoint(cursor.start.point, cursor.end.point, location)
                }
                if (cursor.pathLengthSign * pathLengthSign < 0) {
                    Log.d("predict", "direction changed")
                }
                cursor.state = next
            }
            callback.addAll(found)
        }

        /**
         * 指定された方向へ探索して最近傍点を探す　枝分かれがある場合は枝分かれの数だけ探す
         */
        private fun searchForNearest(
            location: Location,
            previous: PolylineNode,
            current: PolylineNode,
            cursor: PolylineCursor,
            results: MutableCollection<PolylineCursor>,
            pathPosition: Double
        ): Double {
            var min = cursor
            val iterator = current.iterator(previous)
            var minDist = Double.MAX_VALUE
            if (iterator.hasNext()) {
                while (iterator.hasNext()) {
                    val next = iterator.next()
                    val near = NearestPoint(
                        current.point,
                        next.point,
                        LatLng(location.latitude, location.longitude)
                    )
                    if (near.distance > min.nearest.distance * 2) {
                        // 探索終了
                        if (!results.contains(min)) results.add(min)
                        minDist = minDist.coerceAtMost(min.nearest.distance.toDouble())
                    } else {
                        if (near.distance < min.nearest.distance) {
                            min = PolylineCursor(
                                current, next, near,
                                pathPosition,
                                min
                            )
                        }
                        // 深さ優先探索
                        val v = searchForNearest(
                            location,
                            current,
                            next,
                            min,
                            results,
                            pathPosition + iterator.distance() * min.pathLengthSign
                        )
                        minDist = minDist.coerceAtMost(v)
                    }
                }
            } else {
                // 路線の終点 -> 探索終了
                if (!results.contains(min)) results.add(min)
                minDist = min.nearest.distance.toDouble()
            }
            return minDist
        }

        suspend fun predict(result: MutableCollection<StationPrediction>) {
            val list = explorer.search(
                nearest.closedPoint.latitude,
                nearest.closedPoint.longitude,
                1,
                0.0
            )
            if (list.stations.isEmpty()) return
            val s = list.stations[0]
            var cnt = maxPrediction
            val current = if (s != currentStation?.station) {
                result.add(StationPrediction(s, 0f))
                cnt--
                StationArea.parseArea(s)
            } else {
                currentStation
            } ?: throw RuntimeException("nearest station not found")
            searchForStation(start, nearest.closedPoint, end, current, 0f, cnt, result)
        }

        private suspend fun searchForStation(
            previous: PolylineNode,
            startPoint: LatLng,
            end: PolylineNode,
            currentStation: StationArea,
            length: Float,
            remains: Int,
            result: MutableCollection<StationPrediction>
        ) {
            var start = startPoint
            var current = currentStation
            var pathLength = length
            var cnt = remains
            var loop = true
            while (loop) {
                // check start < end
                if (start == end.point) {
                    break
                }
                var a: LatLng =
                    current.points[if (current.enclosed) current.points.size - 1 else 0]
                var i = if (current.enclosed) 0 else 1
                loop = false
                val e1 = Edge(
                    BasePoint(start.longitude, start.latitude),
                    BasePoint(end.point.longitude, end.point.latitude)
                )
                while (i < current.points.size) {
                    val b: LatLng = current.points[i]
                    // 1. Check whether edge start-end goes over boundary a-b
                    val e2 = Edge(
                        BasePoint(a.longitude, a.latitude),
                        BasePoint(b.longitude, b.latitude)
                    )
                    val intersection = e1.getIntersection(e2)
                    // 2. If so, detect the intersection and add to prediction list
                    if (intersection != null &&
                        (intersection.x - start.longitude) * (end.point.longitude - start.longitude) +
                        (intersection.y - start.latitude) * (end.point.latitude - start.latitude) > 0
                    ) {
                        // Calc coordinate of another station
                        var index: Double =
                            (((current.station.lng - b.longitude) * (a.longitude - b.longitude) + (current.station.lat - b.latitude) * (a.latitude - b.latitude))
                                    / ((a.longitude - b.longitude).pow(2.0) + (a.latitude - b.latitude).pow(
                                2.0
                            )))
                        val x = (1 - index) * b.longitude + index * a.longitude
                        val y = (1 - index) * b.latitude + index * a.latitude
                        val lng: Double = 2 * x - current.station.lng
                        val lat: Double = 2 * y - current.station.lat
                        val neighbors = dao.getStations(current.station.next.toList())
                        // Search for which station was detected
                        val next =
                            neighbors.minByOrNull { s -> measureDistance(lat, lng, s.lat, s.lng) }
                                ?.let { s -> StationArea.parseArea(s) }
                                ?: throw RuntimeException("no neighbor found")
                        // Update for next station
                        val dist = measureDistance(
                            start.latitude,
                            start.longitude,
                            intersection.y,
                            intersection.x
                        )
                        val prediction = StationPrediction(next.station, pathLength + dist)
                        result.add(prediction)
                        if (--cnt <= 0) return
                        pathLength += dist
                        index = 1.0 / measureDistance(
                            intersection.y,
                            intersection.x,
                            start.latitude,
                            start.longitude
                        )
                        start = LatLng(
                            (1 + index) * intersection.y - index * start.latitude,
                            (1 + index) * intersection.x - index * start.longitude
                        )
                        current = next
                        loop = true
                        break
                    }
                    a = b
                    i++
                }
            }
            val iterator = end.iterator(previous)
            while (iterator.hasNext()) {
                val next = iterator.next()
                searchForStation(
                    end,
                    end.point,
                    next,
                    current,
                    pathLength + measureDistance(start, end.point),
                    cnt,
                    result
                )
            }
        }

        fun release() {
            start.release()
            end.release()
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as PolylineCursor
            return (start == other.start && end == other.end) ||
                    (start == other.end && end == other.start)
        }

        override fun hashCode(): Int {
            return start.hashCode() + end.hashCode()
        }
    }

    private abstract class PolylineNode(
        val point: LatLng
    ) {
        abstract fun iterator(previous: PolylineNode): NeighborIterator
        abstract fun release()
        abstract fun setNext(next: PolylineNode, distance: Float)

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as PolylineNode
            return this.point == other.point
        }

        override fun hashCode(): Int {
            return point.hashCode()
        }
    }

    private interface NeighborIterator {
        operator fun hasNext(): Boolean
        operator fun next(): PolylineNode
        fun distance(): Float
    }

    private inner class EndNode(
        point: LatLng,
        val segment: PolylineSegment,
        val tag: String
    ) : PolylineNode(point) {

        constructor(
            segment: PolylineSegment,
            cursor: PolylineCursor?
        ) : this(segment.points[0], segment, segment.start) {
            expand(segment, cursor)
        }

        private var next: Array<PolylineNode?> = arrayOfNulls(3)
        private var distance: FloatArray = FloatArray(3)
        private var size = 0
        private var hasChecked = false
        private var hasReleased = false

        override fun setNext(next: PolylineNode, distance: Float) {
            if (size >= 3) {
                throw RuntimeException()
            } else {
                this.next[size] = next
                this.distance[size] = distance
                size++
            }
        }

        private fun expand(segment: PolylineSegment, cursor: PolylineCursor?) {
            val forward: Boolean = segment.start == tag
            val start = if (forward) 1 else segment.points.size - 2
            val end = if (forward) segment.points.size - 1 else 0
            val dir = if (forward) 1 else -1
            var previous: PolylineNode = this
            var index = 0
            var i = start
            while (i != end + dir) {
                val node = if (i == end) EndNode(
                    segment.points[end],
                    segment,
                    if (forward) segment.end else segment.start
                ) else MiddleNode(segment.points[i], index++)
                val distance = measureDistance(previous.point, node.point)
                previous.setNext(node, distance)
                node.setNext(previous, distance)
                if (cursor != null) {
                    if (cursor.nearest.start == previous.point && cursor.nearest.end == node.point) {
                        cursor.initPosition(previous, node)
                    } else if (cursor.nearest.start == node.point && cursor.nearest.end == previous.point) {
                        cursor.initPosition(node, previous)
                    }
                }
                previous = node
                i += dir
            }
        }

        override fun iterator(previous: PolylineNode): NeighborIterator {
            if (size <= 0) throw RuntimeException()
            if (!hasChecked) {
                for (segment in fragmentJunction[tag]!!) {
                    if (segment == this.segment) continue
                    expand(segment, null)
                }
                hasChecked = true
            }
            return JunctionIterator(previous)
        }

        private inner class JunctionIterator(
            val previous: PolylineNode
        ) : NeighborIterator {
            var index = -1
            var nextIndex = -1

            fun searchForNext() {
                nextIndex++
                while (nextIndex < size) {
                    val node = next[nextIndex]
                    val v1 = previous.point
                    val v2 = point
                    val v3 = node!!.point
                    val v = ((v1.longitude - v2.longitude) * (v3.longitude - v2.longitude)
                            + (v1.latitude - v2.latitude) * (v3.latitude - v2.latitude))
                    if (node != previous && v < 0) break
                    nextIndex++
                }
            }

            override fun hasNext(): Boolean {
                return nextIndex < size
            }

            override fun next(): PolylineNode {
                return if (nextIndex >= size) {
                    throw NoSuchElementException()
                } else {
                    index = nextIndex
                    searchForNext()
                    next[index] ?: throw NoSuchElementException()
                }
            }

            override fun distance(): Float {
                return if (index < 0) {
                    throw NoSuchElementException()
                } else {
                    distance[index]
                }
            }

            init {
                searchForNext()
            }
        }

        override fun release() {
            if (!hasReleased) {
                hasReleased = true
                for (i in 0 until size) next[i]?.release()
                hasChecked = false
                size = -1
            }
        }

        override fun toString(): String {
            return java.lang.String.format(
                Locale.US, "EndNode{lat/lon:(%.6f,%.6f), tag:%s, size:%s}",
                point.latitude, point.longitude,
                tag,
                if (hasChecked) size.toString() else "??"
            )
        }
    }

    private class MiddleNode(
        point: LatLng,
        val index: Int
    ) : PolylineNode(point) {

        private var next1: PolylineNode? = null
        private var next2: PolylineNode? = null
        private var distance1 = 0f
        private var distance2 = 0f
        private var hasReleased = false

        override fun setNext(next: PolylineNode, distance: Float) {
            if (next1 == null) {
                next1 = next
                distance1 = distance
            } else if (next2 == null) {
                next2 = next
                distance2 = distance
            } else {
                throw RuntimeException()
            }
        }

        override fun iterator(previous: PolylineNode): NeighborIterator {
            val former = previous == next2
            if (!former && previous != next1) {
                throw RuntimeException()
            }
            return object : NeighborIterator {
                private var hasIterated = false
                override fun hasNext(): Boolean {
                    return !hasIterated
                }

                override fun next(): PolylineNode {
                    return if (hasIterated) {
                        throw NoSuchElementException()
                    } else {
                        hasIterated = true
                        (if (former) next1 else next2) ?: throw IllegalStateException()
                    }
                }

                override fun distance(): Float {
                    return if (hasIterated) {
                        if (former) distance1 else distance2
                    } else {
                        throw NoSuchElementException()
                    }
                }
            }
        }

        override fun release() {
            if (!hasReleased) {
                hasReleased = true
                next1!!.release()
                next2!!.release()
                next1 = null
                next2 = null
            }
        }

        override fun toString(): String {
            return java.lang.String.format(
                Locale.US, "MiddleNode{lat/lon:(%.6f,%.6f), index:%d}",
                point.latitude, point.longitude, index
            )
        }
    }

    class NearestPoint(
        val start: LatLng,
        val end: LatLng,
        point: LatLng
    ) {
        constructor(start: LatLng, end: LatLng, point: Location) : this(
            start,
            end,
            LatLng(point.latitude, point.longitude)
        )

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
            return java.lang.String.format(
                Locale.US,
                "lat/lon:(%.6f,%.6f) - %.2fm",
                closedPoint.latitude, closedPoint.longitude, distance
            )
        }

        init {
            val v1 =
                (point.longitude - start.longitude) * (end.longitude - start.longitude) + (point.latitude - start.latitude) * (end.latitude - start.latitude)
            val v2 =
                (point.longitude - end.longitude) * (start.longitude - end.longitude) + (point.latitude - end.latitude) * (start.latitude - end.latitude)
            if (v1 >= 0 && v2 >= 0) {
                isOnEdge = true
                index = v1 / (Math.pow(start.longitude - end.longitude, 2.0) + Math.pow(
                    start.latitude - end.latitude, 2.0
                ))
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
            distance = measureDistance(closedPoint, point)
            edgeDistance = measureDistance(start, end)
        }
    }


}

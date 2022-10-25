package jp.seo.station.ekisagasu.position

import android.location.Location
import com.google.android.gms.maps.model.LatLng
import jp.seo.android.diagram.Edge
import jp.seo.station.ekisagasu.model.Line
import jp.seo.station.ekisagasu.model.PolylineSegment
import jp.seo.station.ekisagasu.model.Station
import jp.seo.station.ekisagasu.model.StationArea
import jp.seo.station.ekisagasu.position.KalmanFilter.Sample
import jp.seo.station.ekisagasu.search.NearestSearch
import jp.seo.station.ekisagasu.search.measureDistance
import jp.seo.station.ekisagasu.search.measureEuclid
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
import java.util.Locale
import kotlin.math.abs

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
                if (cursors.size > 1) filterCursors(list)
                cursors = list
                Timber.tag("Navigator").d(
                    "cursor size: %d, speed: %.0fkm/h",
                    list.size,
                    list[0].state.speed * 3.6,
                )
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
            }
        }

    private fun initialize(location: Location) {
        polylineFragments.map { f ->
            Pair(
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
                    Timber.tag("Navigator").d("cursor direction changed")
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
            // ポリライン上の現在位置の近傍駅
            val s = explorer.searchEuclid(nearest.closedPoint) ?: return
            var cnt = maxPrediction
            val current = if (s != currentStation?.station) {
                // 現在位置からの近傍駅と異なる場合もある
                result.add(StationPrediction(s, 0f))
                cnt--
                StationArea.parseArea(s)
            } else {
                currentStation
            } ?: throw RuntimeException("nearest station not found")
            searchForStation(start, nearest.closedPoint, end, current, 0f, cnt, result)
        }

        /**
         * 線分start-end上の駅境界を調べて駅の予測を追加する
         * @param start
         * @param end
         * @param current 探索中の現在の近傍駅
         * @param pathLength ポリライン上の現在位置から起算した探索点の距離（予測の表示用）
         * @param cnt あと何個予測を追加するか
         */
        private suspend fun searchForStation(
            previous: PolylineNode,
            start: LatLng,
            end: PolylineNode,
            current: StationArea,
            pathLength: Float,
            cnt: Int,
            result: MutableCollection<StationPrediction>,
            depth: Int = 0,
        ) {
            // TODO 無限ループ回避策（消極的）
            if (depth > 1000) {
                Timber.tag("Navigator").w("searchForStation 呼び出し回数が深すぎます！探索を強制終了します")
                return
            }

            assert {
                val s = explorer.searchEuclid(start)
                if (s != current.station) {
                    Timber.tag("Navigator").w(
                        "station mismatch! current: ${current.station.name}, start: ${s?.name}",
                    )
                }
                true
            }

            var intersectionFound = false
            if (start != end.point) {
                val stationAtEnd = requireNotNull(explorer.searchEuclid(end.point))
                // ボロノイ領域は凸なので線分の両端で同じ駅なら線分上どこでも同じ駅
                // 逆に両端で異なるなら線分上に１つ以上の境界線との交点がある
                if (stationAtEnd != current.station) {
                    intersectionFound = true
                    val edge = Edge(start.point2D, end.point.point2D)
                    val intersection = current.getIntersection(edge)?.latLng ?: kotlin.run {
                        // TODO 端点が境界に非常に近い場合、見つからない場合あがある？
                        val middle = edge.middlePoint
                        val stationAtMiddle = explorer.searchEuclid(middle.latLng)
                        if (stationAtEnd == stationAtMiddle) {
                            start
                        } else if (current.station == stationAtMiddle) {
                            end.point
                        } else {
                            throw RuntimeException("intersection with station voronoi not found")
                        }
                    }
                    /*
                       次の探索開始点＆隣接駅を決定
                       交点だと現在の駅と隣接駅から距離が同じで不都合
                       start --> end 方向に見てdelta[m]だけ先の点を選ぶ
                       線分i-startを(-delta) : (delta + distFromStart)の内分点
                     */
                    val distToEnd = end.point.measureEuclid(intersection)
                    val distFromStart = start.measureEuclid(intersection)
                    val delta = 0.00001
                    val nextStart = if (intersection != end.point && delta < distToEnd) {
                        val m = delta / distFromStart
                        LatLng(
                            (1 + m) * intersection.latitude - m * start.latitude,
                            (1 + m) * intersection.longitude - m * start.longitude
                        )
                    } else {
                        // 終点endが近すぎる場合
                        end.point
                    }
                    // 隣接駅
                    val station = requireNotNull(explorer.searchEuclid(nextStart))
                    // 次の探索駅
                    val next = StationArea.parseArea(station)
                    // 表示用の距離
                    val nextPathLength =
                        pathLength + start.measureDistance(nextStart)
                    // 予測を追加
                    val prediction = StationPrediction(station, nextPathLength)
                    result.add(prediction)

                    if (cnt - 1 > 0) {
                        // まだ必要なら残りの線分上を探索続ける
                        searchForStation(
                            previous,
                            nextStart,
                            end,
                            next,
                            nextPathLength,
                            cnt - 1,
                            result,
                            depth + 1,
                        )
                    }
                }
            }

            // 線分上に駅境界を発見しているなら探索済み
            if (intersectionFound) return

            // 線分上に駅境界が見つからなかった場合は、次のポリライン線分を探索
            assert(cnt > 0)
            val iterator = end.iterator(previous)
            while (iterator.hasNext()) {
                val next = iterator.next()
                assert {
                    val s = explorer.searchEuclid(end.point)
                    if (s != current.station) {
                        Timber.tag("Navigator").w(
                            "station mismatch! current: ${current.station.name}, end: ${s?.name}",
                        )
                    }
                    true
                }
                searchForStation(
                    end,
                    end.point,
                    next,
                    current,
                    pathLength + start.measureDistance(end.point),
                    cnt,
                    result,
                    depth + 1,
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
                val distance = previous.point.measureDistance(node.point)
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
                    val v = (
                            (v1.longitude - v2.longitude) * (v3.longitude - v2.longitude) +
                                    (v1.latitude - v2.latitude) * (v3.latitude - v2.latitude)
                            )
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
            return String.format(
                Locale.US, "EndNode(lat/lon:(%.6f,%.6f), tag:%s, size:%s)",
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
            return String.format(
                Locale.US, "MiddleNode(lat/lon:(%.6f,%.6f), index:%d)",
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
            return String.format(
                Locale.US,
                "NearestPoint(lat/lon:(%.6f,%.6f) - %.2fm)",
                closedPoint.latitude, closedPoint.longitude, distance
            )
        }

        init {
            val v1 = (point.longitude - start.longitude) * (end.longitude - start.longitude) +
                    (point.latitude - start.latitude) * (end.latitude - start.latitude)
            val v2 = (point.longitude - end.longitude) * (start.longitude - end.longitude) +
                    (point.latitude - end.latitude) * (start.latitude - end.latitude)
            if (v1 >= 0 && v2 >= 0) {
                isOnEdge = true
                index = v1 /
                        Math.pow(start.longitude - end.longitude, 2.0) + Math.pow(start.latitude - end.latitude, 2.0)
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
            distance = closedPoint.measureDistance(point)
            edgeDistance = start.measureDistance(end)
        }
    }
}

package com.seo4d696b75.android.ekisagasu.data.polyline

import com.google.android.gms.maps.model.LatLng
import com.seo4d696b75.android.ekisagasu.data.kdtree.measureDistance
import com.seo4d696b75.android.ekisagasu.data.kdtree.measureEuclid
import com.seo4d696b75.android.ekisagasu.domain.kdtree.NearestSearch
import com.seo4d696b75.android.ekisagasu.domain.location.Location
import jp.seo.diagram.core.Edge
import timber.log.Timber
import kotlin.math.max
import kotlin.math.min

class PolylineCursor {

    companion object {
        fun initialize(
            segment: PolylineSegment,
            provider: PolylineSegmentProvider,
            nearest: NearestPoint,
            explore: NearestSearch,
        ): PolylineCursor {
            val (start, end) = PolylineEndNode.initialize(segment, provider, nearest)
            return PolylineCursor(start, end, nearest, explore)
        }
    }

    /**
     * 初期化
     */
    private constructor(
        start: PolylineNode,
        end: PolylineNode,
        nearest: NearestPoint,
        explore: NearestSearch,
    ) {
        this.start = start
        this.end = end
        this.nearest = nearest
        this.explorer = explore

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
        old: PolylineCursor,
    ) {
        this.nearest = nearest
        this.explorer = old.explorer
        this.start = start
        this.end = end
        require(!(start.point != nearest.start || end.point != nearest.end))
        pathPosAtStart = pathPosition
        pathLengthSign = old.pathLengthSign
        pathPosAtNearest = pathPosition + nearest.distanceFrom() * pathLengthSign

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

        val position = LatLng(location.lat, location.lng)
        nearest = NearestPoint.from(start.point, end.point, position)
        this.explorer = old.explorer
        isSignDecided = old.isSignDecided
        pathPosAtNearest = pathPosAtStart + nearest.distanceFrom() * pathLengthSign
    }

    private val explorer: NearestSearch

    val nearest: NearestPoint

    private val start: PolylineNode
    private val end: PolylineNode

    // start -> end 方向を正方向とし、startを原点とする1次元座標で測定する
    private var pathLengthSign = 0
    private var pathPosAtStart = 0.0
    private var pathPosAtNearest: Double
    private var isSignDecided = false

    fun update(
        location: Location,
        callback: MutableCollection<PolylineCursor>,
    ) {
        // Search for the nearest point on this polyline to the given location in all the directions
        val list1 = mutableListOf<PolylineCursor>()
        val list2 = mutableListOf<PolylineCursor>()
        val v1 =
            searchForNearest(
                location,
                start,
                end,
                PolylineCursor(this, true, location),
                list1,
                pathPosAtStart + nearest.edgeDistance * pathLengthSign,
            )
        val v2 =
            searchForNearest(
                location,
                end,
                start,
                PolylineCursor(this, false, location),
                list2,
                pathPosAtStart,
            )
        Timber.tag("PolylineCursor").d("updated v1:$v1 v2:$v2")
        val found: List<PolylineCursor> =
            if (max(v1, v2) / min(v1, v2) >= 2.0) {
                if (v1 < v2) list1 else list2
            } else {
                list1
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
        pathPosition: Double,
    ): Double {
        var min = cursor
        val iterator = current.iterator(previous)
        var minDist = Double.MAX_VALUE
        if (iterator.hasNext()) {
            while (iterator.hasNext()) {
                val next = iterator.next()
                val near =
                    NearestPoint.from(
                        current.point,
                        next.point,
                        LatLng(location.lat, location.lng),
                    )
                if (near.distance > min.nearest.distance * 2) {
                    // 探索終了
                    if (!results.contains(min)) results.add(min)
                    minDist = minDist.coerceAtMost(min.nearest.distance.toDouble())
                } else {
                    if (near.distance < min.nearest.distance) {
                        min =
                            PolylineCursor(
                                current, next, near,
                                pathPosition,
                                min,
                            )
                    }
                    // 深さ優先探索
                    val v =
                        searchForNearest(
                            location,
                            current,
                            next,
                            min,
                            results,
                            pathPosition + iterator.distance() * min.pathLengthSign,
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

    suspend fun predict(
        result: MutableCollection<StationPrediction>,
        maxPrediction: Int,
    ) {
        // ポリライン上の現在位置の近傍駅
        val s = explorer.searchEuclid(nearest.nearest) ?: return
        val area = StationArea.parseArea(s)
        searchForStation(
            previous = start,
            start = nearest.nearest,
            end = end,
            current = area,
            pathLength = 0f,
            cnt = maxPrediction,
            result = result,
            depth = 0,
        )
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
        depth: Int,
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

        require(cnt > 0)

        if (start != end.point) {
            val stationAtEnd = requireNotNull(explorer.searchEuclid(end.point))
            // ボロノイ領域は凸なので線分の両端で同じ駅なら線分上どこでも同じ駅
            // 逆に両端で異なるなら線分上に１つ以上の境界線との交点がある
            if (stationAtEnd != current.station) {
                val edge = Edge(start.point2D, end.point.point2D)
                val intersection =
                    current.getIntersection(edge)?.latLng ?: kotlin.run {
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
                    require(!m.isNaN())
                    LatLng(
                        (1 + m) * intersection.latitude - m * start.latitude,
                        (1 + m) * intersection.longitude - m * start.longitude,
                    )
                } else {
                    // 終点endが近すぎる場合
                    end.point
                }
                // 隣接駅
                val station = requireNotNull(explorer.searchEuclid(nextStart))
                require(station != current.station)
                // 次の探索駅
                val next = StationArea.parseArea(station)
                // 表示用の距離
                val nextPathLength = pathLength + start.measureDistance(nextStart)
                // 予測を追加
                val prediction = StationPrediction(station, nextPathLength)
                result.add(prediction)

                if (cnt - 1 > 0) {
                    // まだ必要なら残りの線分上を探索続ける
                    searchForStation(
                        previous = previous,
                        start = nextStart,
                        end = end,
                        current = next,
                        pathLength = nextPathLength,
                        cnt = cnt - 1,
                        result = result,
                        depth = depth + 1,
                    )
                }
                return
            }
        }

        // 線分上に駅境界が見つからなかった場合は、次のポリライン線分を探索
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
                previous = end,
                start = end.point,
                end = next,
                current = current,
                pathLength = pathLength + start.measureDistance(end.point),
                cnt = cnt,
                result = result,
                depth = depth + 1,
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

    override fun hashCode(): Int = start.hashCode() + end.hashCode()
}

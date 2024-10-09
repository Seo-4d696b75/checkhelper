package com.seo4d696b75.android.ekisagasu.data.polyline

import com.google.android.gms.maps.model.LatLng
import com.seo4d696b75.android.ekisagasu.data.kdtree.measureDistance
import java.util.Locale

typealias PolylineSegmentProvider = (String) -> List<PolylineSegment>

class PolylineEndNode private constructor(
    point: LatLng,
    private val segment: PolylineSegment,
    private val provider: PolylineSegmentProvider,
    private val tag: String,
) : PolylineNode(point) {
    companion object {
        // 指定した1辺分のグラフを初期化
        fun initialize(
            segment: PolylineSegment,
            provider: PolylineSegmentProvider,
            nearest: NearestPoint,
        ): Pair<PolylineNode, PolylineNode> {
            val node = PolylineEndNode(
                point = segment.points[0],
                segment = segment,
                provider = provider,
                tag = segment.start,
            ).apply {
                expand(segment)
            }
            var previous: PolylineNode = node
            var current: PolylineNode = requireNotNull(node.next[0])
            while (true) {
                if (nearest.start == previous.point && nearest.end == current.point) {
                    return previous to current
                } else if (nearest.start == current.point && nearest.end == previous.point) {
                    return current to previous
                }
                if (current is PolylineMiddleNode) {
                    val iterator = current.iterator(previous)
                    previous = current
                    current = iterator.next()
                } else {
                    break
                }
            }
            throw NoSuchElementException()
        }
    }

    private val next: Array<PolylineNode?> = arrayOfNulls(3)
    private var distance: FloatArray = FloatArray(3)
    private var size = 0
    private var hasChecked = false
    private var hasReleased = false

    override fun setNext(
        next: PolylineNode,
        distance: Float,
    ) {
        require(!next.point.latitude.isNaN() && !next.point.longitude.isNaN())
        if (size >= 3) {
            throw IllegalStateException("neighbor size is already 3")
        } else {
            this.next[size] = next
            this.distance[size] = distance
            size++
        }
    }

    private fun expand(
        segment: PolylineSegment,
    ) {
        val forward: Boolean = segment.start == tag
        val start = if (forward) 1 else segment.points.size - 2
        val end = if (forward) segment.points.size - 1 else 0
        val dir = if (forward) 1 else -1
        var previous: PolylineNode = this
        var index = 0
        var i = start
        while (i != end + dir) {
            val node = if (i == end) {
                PolylineEndNode(
                    segment.points[end],
                    segment,
                    provider,
                    if (forward) segment.end else segment.start,
                )
            } else {
                PolylineMiddleNode(segment.points[i], index++)
            }
            val distance = previous.point.measureDistance(node.point)
            previous.setNext(node, distance)
            node.setNext(previous, distance)
            previous = node
            i += dir
        }
    }

    override fun iterator(previous: PolylineNode): NeighborIterator {
        if (size <= 0) throw RuntimeException()
        if (!hasChecked) {
            for (segment in provider(tag)) {
                if (segment == this.segment) continue
                expand(segment)
            }
            hasChecked = true
        }
        return JunctionIterator(previous)
    }

    private inner class JunctionIterator(val previous: PolylineNode) : NeighborIterator {
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

        override fun hasNext(): Boolean = nextIndex < size

        override fun next(): PolylineNode = if (nextIndex >= size) {
            throw NoSuchElementException()
        } else {
            index = nextIndex
            searchForNext()
            next[index] ?: throw NoSuchElementException()
        }

        override fun distance(): Float = if (index < 0) {
            throw NoSuchElementException()
        } else {
            distance[index]
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

    override fun toString(): String = String.format(
        Locale.US,
        "EndNode(lat/lon:(%.6f,%.6f), tag:%s, size:%s)",
        point.latitude,
        point.longitude,
        tag,
        if (hasChecked) size.toString() else "??",
    )
}

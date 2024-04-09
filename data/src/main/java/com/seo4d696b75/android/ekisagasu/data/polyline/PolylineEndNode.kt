package com.seo4d696b75.android.ekisagasu.data.polyline

import com.google.android.gms.maps.model.LatLng
import com.seo4d696b75.android.ekisagasu.data.kdtree.measureDistance
import java.util.Locale

typealias PolylineSegmentProvider = (String) -> List<PolylineSegment>

class PolylineEndNode(
    point: LatLng,
    private val segment: PolylineSegment,
    private val provider: PolylineSegmentProvider,
    val tag: String,
) : PolylineNode(point) {
    constructor(
        segment: PolylineSegment,
        provider: PolylineSegmentProvider,
        cursor: PolylineCursor?,
    ) : this(segment.points[0], segment, provider, segment.start) {
        expand(segment, cursor)
    }

    private var next: Array<PolylineNode?> = arrayOfNulls(3)
    private var distance: FloatArray = FloatArray(3)
    private var size = 0
    private var hasChecked = false
    private var hasReleased = false

    override fun setNext(
        next: PolylineNode,
        distance: Float,
    ) {
        if (size >= 3) {
            throw RuntimeException()
        } else {
            this.next[size] = next
            this.distance[size] = distance
            size++
        }
    }

    private fun expand(
        segment: PolylineSegment,
        cursor: PolylineCursor?,
    ) {
        val forward: Boolean = segment.start == tag
        val start = if (forward) 1 else segment.points.size - 2
        val end = if (forward) segment.points.size - 1 else 0
        val dir = if (forward) 1 else -1
        var previous: PolylineNode = this
        var index = 0
        var i = start
        while (i != end + dir) {
            val node =
                if (i == end) {
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
            for (segment in provider(tag)) {
                if (segment == this.segment) continue
                expand(segment, null)
            }
            hasChecked = true
        }
        return JunctionIterator(previous)
    }

    private inner class JunctionIterator(
        val previous: PolylineNode,
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
            Locale.US,
            "EndNode(lat/lon:(%.6f,%.6f), tag:%s, size:%s)",
            point.latitude,
            point.longitude,
            tag,
            if (hasChecked) size.toString() else "??",
        )
    }
}

package com.seo4d696b75.android.ekisagasu.data.polyline

import com.google.android.gms.maps.model.LatLng
import java.util.Locale

class PolylineMiddleNode(
    point: LatLng,
    val index: Int,
) : PolylineNode(point) {
    private var next1: PolylineNode? = null
    private var next2: PolylineNode? = null
    private var distance1 = 0f
    private var distance2 = 0f
    private var hasReleased = false

    override fun setNext(
        next: PolylineNode,
        distance: Float,
    ) {
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
            Locale.US,
            "MiddleNode(lat/lon:(%.6f,%.6f), index:%d)",
            point.latitude,
            point.longitude,
            index,
        )
    }
}

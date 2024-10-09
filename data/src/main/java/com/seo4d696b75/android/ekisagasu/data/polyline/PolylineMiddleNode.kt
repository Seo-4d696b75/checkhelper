package com.seo4d696b75.android.ekisagasu.data.polyline

import com.google.android.gms.maps.model.LatLng
import java.util.Locale

class PolylineMiddleNode(point: LatLng, val index: Int) : PolylineNode(point) {
    private var next1: PolylineNode? = null
    private var next2: PolylineNode? = null
    private var distance1 = 0f
    private var distance2 = 0f
    private var hasReleased = false

    override fun setNext(
        next: PolylineNode,
        distance: Float,
    ) {
        require(!next.point.latitude.isNaN() && !next.point.longitude.isNaN())

        if (next1 == null) {
            next1 = next
            distance1 = distance
        } else if (next2 == null) {
            next2 = next
            distance2 = distance
        } else {
            throw IllegalArgumentException("already init both neighbors")
        }
    }

    override fun iterator(previous: PolylineNode): NeighborIterator {
        if (next1 == null && next2 == null) {
            throw IllegalStateException("node not init yet")
        }
        val former = previous == next2
        require(former || previous == next1) {
            "previous($previous) is not either of neighbors"
        }
        return object : NeighborIterator {
            private var hasIterated = false

            override fun hasNext(): Boolean = !hasIterated

            override fun next(): PolylineNode = if (hasIterated) {
                throw NoSuchElementException()
            } else {
                hasIterated = true
                requireNotNull(
                    if (former) next1 else next2
                )
            }

            override fun distance(): Float = if (hasIterated) {
                if (former) distance1 else distance2
            } else {
                throw NoSuchElementException()
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

    override fun toString(): String = String.format(
        Locale.US,
        "MiddleNode(lat/lon:(%.6f,%.6f), index:%d)",
        point.latitude,
        point.longitude,
        index,
    )
}

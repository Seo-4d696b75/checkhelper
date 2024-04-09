package com.seo4d696b75.android.ekisagasu.data.polyline

import com.google.android.gms.maps.model.LatLng

abstract class PolylineNode(
    val point: LatLng,
) {
    abstract fun iterator(previous: PolylineNode): NeighborIterator

    abstract fun release()

    abstract fun setNext(
        next: PolylineNode,
        distance: Float,
    )

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

package com.seo4d696b75.android.ekisagasu.data.polyline

interface NeighborIterator {
    operator fun hasNext(): Boolean

    operator fun next(): PolylineNode

    fun distance(): Float
}

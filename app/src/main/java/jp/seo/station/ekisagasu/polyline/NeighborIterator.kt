package jp.seo.station.ekisagasu.polyline

interface NeighborIterator {
    operator fun hasNext(): Boolean

    operator fun next(): PolylineNode

    fun distance(): Float
}

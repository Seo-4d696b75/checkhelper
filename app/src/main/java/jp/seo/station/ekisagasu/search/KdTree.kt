package jp.seo.station.ekisagasu.search

import jp.seo.station.ekisagasu.Station
import jp.seo.station.ekisagasu.core.StationDao
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.math.*

/**
 * @author Seo-4d696b75
 * @version 2020/12/18.
 * kd-treeによる最近傍探索データ構造.
 * 内部で計算する距離の種類:
 * (1) sphere == false 緯度経度をそのまま直交座標系に投影した平面状でのユークリッド距離
 * (2) sphere == true  地球を完全な球体と仮定して計算した大円距離
 */
class KdTree(
    private val database: StationDao,
) {

    private var _root: Node? = null

    // mutex lock obj when checking and loading tree-segment data, in order to avoid duplicated operations
    private val lock = Mutex()

    private suspend fun getRoot(): Node = lock.withLock{
        _root?: run{
            val data = database.getTreeSegment("root")
            val map: MutableMap<Int, StationNode> = HashMap()
            for (s in data.nodes) {
                map[s.code] = s
            }
            val node = Node(map.getValue(data.root), map, 0)
            _root = node
            node
        }
    }

    companion object {

        const val SPHERE_RADIUS = 6371009.0
    }

    private inner class Node(
        data: StationNode,
        nodes: Map<Int, StationNode>,
        val depth: Int
    ) {

        val stationCode = data.code
        val lat = data.lat
        val lng = data.lng


        private var segmentName: String? = null
        var left: Node? = null
        var right: Node? = null

        init {
            build(data, nodes)
        }

        private fun build(data: StationNode, nodes: Map<Int, StationNode>) {
            if (data.segment != null) {
                segmentName = data.segment
            } else {
                data.left?.let {
                    val leftData = nodes.getValue(it)
                    this.left = Node(leftData, nodes, depth + 1)
                }
                data.right?.let {
                    val rightData = nodes.getValue(it)
                    this.right = Node(rightData, nodes, depth + 1)
                }
            }
        }

        suspend fun traverse() = lock.withLock {
            segmentName?.let {
                val segment = database.getTreeSegment(it)
                if (segment.root != this.stationCode) throw RuntimeException("root mismatch name:$it")
                val map: MutableMap<Int, StationNode> = HashMap<Int, StationNode>()
                for (s in segment.nodes) {
                    map[s.code] = s
                }
                build(map.getValue(this.stationCode), map)
                segmentName = null
            }
        }
    }

    private data class SearchProperties(
        val lat: Double,
        val lng: Double,
        val k: Int,
        val r: Double,
        val sphere: Boolean,
    ) {
        val list: MutableList<NeighborNode> = ArrayList()
    }

    private data class NeighborNode(
        val code: Int,
        val dist: Double
    )

    class SearchResult(
        val lat: Double,
        val lng: Double,
        val k: Int,
        val r: Double,
        val stations: List<Station>
    )

    suspend fun search(
        lat: Double,
        lng: Double,
        k: Int,
        r: Double,
        sphere: Boolean = false
    ): SearchResult {
        val prop = SearchProperties(lat, lng, k, r, sphere)
        search(getRoot(), prop)
        val indices = prop.list.map { n -> n.code }
        val data = database.getStations(indices)
        val stations = indices.map { code ->
            data.find { s -> s.code == code } ?: throw NoSuchElementException()
        }
        return SearchResult(lat, lng, k, r, stations)
    }

    private suspend fun search(node: Node?, prop: SearchProperties) {
        if (node == null) return
        // be sure to call traverse()
        node.traverse()
        val d = measure(prop.lng, prop.lat, node.lng, node.lat, prop.sphere)
        var index = -1
        val size = prop.list.size
        if (size > 0 && d < prop.list[size - 1].dist) {
            index = size - 1
            while (index > 0) {
                if (d >= prop.list[index - 1].dist) break
                index--
            }
        } else if (size == 0) {
            index = 0
        }
        if (index >= 0) {
            prop.list.add(index, NeighborNode(node.stationCode, d))

            /*
            prop.list に距離昇順に格納された結果に関して、
            (i) サイズが k 以上
            (ii) 距離 r 以内はすべて含まれる
            の両条件を満たす
             */
            if (size >= prop.k && prop.list[size].dist > prop.r) {
                prop.list.removeAt(size)
            }
        }
        val x = (node.depth % 2 == 0)
        val value = if (x) prop.lng else prop.lat
        val threshold = if (x) node.lng else node.lat
        search(if (value < threshold) node.left else node.right, prop)
        /*
         calc closed distance to boundary, which is
           (1) x == true    line of longitude = node.lng
           (2) x == false   line of latitude = node.lat
         */
        val closedDist = if (prop.sphere) {
            if (x) {
                /*
                 経線までの距離は緯線にはならない（緯度依存あり）
                 球面三角法で計算
                 */
                val x = PI * abs(prop.lng - node.lng) / 180.0
                val y = PI * prop.lat / 180.0
                SPHERE_RADIUS * asin(sin(x) * cos(y))
            } else {
                /*
                 緯線は極を中心とする円だから座標(prop.lat, prop.lng)と極を結ぶ直線上での目標緯度までの距離
                 その直線は経線だから単純に緯度差が中心を見込む角になる
                 */
                SPHERE_RADIUS * PI * abs(prop.lat - node.lat) / 180.0
            }
        } else {
            /*
            直交座標系を仮定する場合は単純なユークリッド距離の大小で考えるため、
            比較座標値の差の絶対値
             */
            abs(value - threshold)
        }

        if (closedDist < max(prop.list[prop.list.size - 1].dist, prop.r)) {
            search(if (value < threshold) node.right else node.left, prop)
        }
    }


    private inline fun measure(
        x0: Double,
        y0: Double,
        x1: Double,
        y1: Double,
        sphere: Boolean,
    ): Double {
        return if (sphere) {
            val lng1 = PI * x0 / 180.0
            val lat1 = PI * y0 / 180.0
            val lng2 = PI * x1 / 180.0
            val lat2 = PI * y1 / 180.0
            val lng = (lng1 - lng2) / 2
            val lat = (lat1 - lat2) / 2
            return SPHERE_RADIUS * 2 * asin(
                sqrt(
                    sin(lat).pow(2) + cos(lat1) * cos(lat2) * sin(lng).pow(
                        2
                    )
                )
            )
        } else {
            sqrt((x0 - x1).pow(2) + (y0 - y1).pow(2))
        }
    }

}



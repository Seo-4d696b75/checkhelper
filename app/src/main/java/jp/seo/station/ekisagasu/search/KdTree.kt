package jp.seo.station.ekisagasu.search

import jp.seo.station.ekisagasu.database.StationDao
import jp.seo.station.ekisagasu.model.StationNode
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import kotlin.math.*

/**
 * @author Seo-4d696b75
 * @version 2020/12/18.
 * kd-treeによる最近傍探索データ構造.
 * 内部で計算する距離の種類:
 * (1) sphere == false 緯度経度をそのまま直交座標系に投影した平面状でのユークリッド距離
 * (2) sphere == true  地球を完全な球体と仮定して計算した大円距離
 */
class KdTree @Inject constructor(
    private val database: StationDao,
) : NearestSearch {

    private var _root: NodeAdapter? = null

    // mutex lock obj when checking and loading tree-segment data, in order to avoid duplicated operations
    private val lock = Mutex()

    private suspend fun getRoot(): NodeAdapter = lock.withLock {
        _root ?: run {
            val data = database.getTreeSegment("root")
            val map: MutableMap<Int, StationNode> = HashMap()
            for (s in data.nodes) {
                map[s.code] = s
            }
            val node = NodeAdapter(0, data.root, map)
            _root = node
            node
        }
    }

    private data class Node(
        val depth: Int,
        val lat: Double,
        val lng: Double,
        val code: Int,
        val left: NodeAdapter?,
        val right: NodeAdapter?
    )

    private inner class NodeAdapter(
        val depth: Int,
        val code: Int,
        nodes: Map<Int, StationNode>
    ) {

        private var _node: Node? = null
        private var segmentName: String? = null

        init {
            build(nodes)
        }

        private fun build(nodes: Map<Int, StationNode>) {
            val data = nodes.getValue(code)
            if (data.segment != null) {
                segmentName = data.segment
            } else {
                val left = data.left?.let {
                    NodeAdapter(depth + 1, it, nodes)
                }
                val right = data.right?.let {
                    NodeAdapter(depth + 1, it, nodes)
                }
                val lat = data.lat ?: throw RuntimeException("lat not found")
                val lng = data.lng ?: throw RuntimeException("lng not found")
                this._node = Node(depth, lat, lng, code, left, right)
            }
        }

        suspend fun node(): Node = lock.withLock {
            _node ?: kotlin.run {
                val segment = database.getTreeSegment(
                    segmentName ?: throw RuntimeException("segment-name not found")
                )
                if (segment.root != this.code) throw RuntimeException("root mismatch name:$segmentName")
                val map: MutableMap<Int, StationNode> = HashMap<Int, StationNode>()
                for (s in segment.nodes) {
                    map[s.code] = s
                }
                build(map)
                val node = _node ?: throw RuntimeException("node not initialized")
                segmentName = null
                node
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

    override suspend fun search(
        lat: Double,
        lng: Double,
        k: Int,
        r: Double,
        sphere: Boolean,
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

    private suspend fun search(adapter: NodeAdapter?, prop: SearchProperties) {
        if (adapter == null) return
        val node = adapter.node()
        val d = measure(prop.lat, prop.lng, node.lat, node.lng, prop.sphere)
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
            prop.list.add(index, NeighborNode(node.code, d))

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
                val lng = PI * abs(prop.lng - node.lng) / 180.0
                val lat = PI * prop.lat / 180.0
                SPHERE_RADIUS * asin(sin(lng) * cos(lat))
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


}



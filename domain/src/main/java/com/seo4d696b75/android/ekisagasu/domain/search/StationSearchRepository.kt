package com.seo4d696b75.android.ekisagasu.domain.search

import com.seo4d696b75.android.ekisagasu.domain.dataset.Line
import com.seo4d696b75.android.ekisagasu.domain.location.Location
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * 近傍の駅の探索機能を提供
 */
interface StationSearchRepository {
    fun setSearchK(value: Int)
    val result: Flow<StationSearchResult?>
    fun selectLine(line: Line?)
    val selectedLine: StateFlow<Line?>
}

data class StationSearchResult(
    val location: Location,
    val searchK: Int,
    /**
     * 現在位置からの最近傍の駅
     * [nearest]とは異なり現在位置が変化しても更新されず、駅が変化したタイミングでのみ更新される
     * [NearStation]の距離・タイムスタンプは更新されたときの値のまま保持される
     */
    val detected: NearStation,

    /**
     * 現在位置からの近傍駅を近い順にソートしたリスト
     */
    val nears: List<NearStation>,
) {

    /**
     * 現在位置から最近傍の駅と距離情報
     */
    val nearest: NearStation
        get() = nears[0]
}

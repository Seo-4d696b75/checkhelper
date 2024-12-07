package com.seo4d696b75.android.ekisagasu.domain.search

import com.seo4d696b75.android.ekisagasu.domain.location.Location

sealed interface StationSearchState {
    val searchK: Int

    data class Idle(
        override val searchK: Int,
    ) : StationSearchState

    data class Initializing(
        override val searchK: Int,
    ) : StationSearchState

    data class Result(
        override val searchK: Int,
        val location: Location,
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
    ) : StationSearchState {
        /**
         * 現在位置から最近傍の駅と距離情報
         */
        val nearest: NearStation
            get() = nears.first()
    }
}

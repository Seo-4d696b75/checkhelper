package jp.seo.station.ekisagasu.repository

import android.location.Location
import jp.seo.station.ekisagasu.model.Line
import jp.seo.station.ekisagasu.model.NearStation
import kotlinx.coroutines.flow.StateFlow

/**
 * 近傍の駅の探索機能を提供
 */
interface SearchRepository {
    suspend fun setSearchK(value: Int)
    suspend fun updateNearestStations(location: Location)
    fun selectLine(line: Line?)
    fun onStopSearch()

    /**
     * 現在位置から最近傍の駅と距離情報
     * 探索を開始し位置情報を更新された状態でのみ `not-Null`
     */
    val nearestStation: StateFlow<NearStation?>

    val selectedLine: StateFlow<Line?>

    /**
     * 現在位置からの近傍駅を近い順にソートしたリスト
     */
    val nearestStations: StateFlow<List<NearStation>>

    /**
     * 現在位置からの最近傍の駅
     * [nearestStation]とは異なり現在位置が変化しても更新されず、駅が変化したタイミングでのみ更新される
     * [NearStation]の距離・タイムスタンプは更新されたときの値のまま保持される
     */
    val detectedStation: StateFlow<NearStation?>
}

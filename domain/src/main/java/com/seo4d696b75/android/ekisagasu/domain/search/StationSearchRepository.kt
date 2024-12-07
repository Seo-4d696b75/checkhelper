package com.seo4d696b75.android.ekisagasu.domain.search

import com.seo4d696b75.android.ekisagasu.domain.dataset.Line
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * 近傍の駅の探索機能を提供
 */
interface StationSearchRepository {
    val state: Flow<StationSearchState>
    val selectedLine: StateFlow<Line?>
    suspend fun selectLine(line: Line)
    fun clearLine()
}

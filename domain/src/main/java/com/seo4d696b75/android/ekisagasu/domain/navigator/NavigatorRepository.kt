package com.seo4d696b75.android.ekisagasu.domain.navigator

import com.seo4d696b75.android.ekisagasu.domain.dataset.Line
import kotlinx.coroutines.flow.Flow

/**
 * 現在位置・乗車中路線から次の駅を予測する
 *
 * @author Seo-4d696b75
 * @version 2021/03/05.
 */
interface NavigatorRepository {
    val state: Flow<NavigatorState>
    fun start(line: Line)
    fun stop()
}

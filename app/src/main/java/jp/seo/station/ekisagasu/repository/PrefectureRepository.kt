package jp.seo.station.ekisagasu.repository

import android.content.Context

/**
 * @author Seo-4d696b75
 * @version 2021/01/13.
 */
interface PrefectureRepository {
    suspend fun setData(context: Context)
    fun getName(code: Int): String
}

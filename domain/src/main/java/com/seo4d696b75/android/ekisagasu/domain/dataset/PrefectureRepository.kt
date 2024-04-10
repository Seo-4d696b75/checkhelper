package com.seo4d696b75.android.ekisagasu.domain.dataset

/**
 * @author Seo-4d696b75
 * @version 2021/01/13.
 */
interface PrefectureRepository {
    suspend fun setData()

    fun getName(code: Int): String
}

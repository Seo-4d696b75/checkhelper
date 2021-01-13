package jp.seo.station.ekisagasu.core

import android.content.Context
import android.util.SparseArray
import jp.seo.station.ekisagasu.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * @author Seo-4d696b75
 * @version 2021/01/13.
 */
class PrefectureRepository {

    private var _prefectures: SparseArray<String>? = null

    suspend fun setData(context: Context) = withContext(Dispatchers.IO) {
        try {
            val stream = context.resources.openRawResource(R.raw.prefecture)
            val reader = stream.bufferedReader()
            val array = SparseArray<String>()
            reader.lines().forEach { line ->
                val data = line.split(",")
                if (data.size == 2) {
                    val id = data[0].toInt()
                    array.put(id, data[1])
                }
            }
            _prefectures = array
        } catch (e: Exception) {
            throw RuntimeException("fail to init prefecture names")
        }
    }

    fun getName(code: Int): String {
        if (code < 1 || code > 47) return "unknown"
        return _prefectures?.get(code) ?: "not-init"
    }

}

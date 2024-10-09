package com.seo4d696b75.android.ekisagasu.data.station

import android.content.Context
import android.util.SparseArray
import com.seo4d696b75.android.ekisagasu.data.R
import com.seo4d696b75.android.ekisagasu.domain.dataset.PrefectureRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

class PrefectureRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : PrefectureRepository {
    private var _prefectures: SparseArray<String>? = null

    override suspend fun setData() = withContext(Dispatchers.IO) {
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

    override fun getName(code: Int): String {
        if (code < 1 || code > 47) return "unknown"
        return _prefectures?.get(code) ?: "not-init"
    }
}

@Suppress("unused")
@Module
@InstallIn(SingletonComponent::class)
interface PrefectureRepositoryModule {
    @Singleton
    @Binds
    fun bindPrefectureRepository(impl: PrefectureRepositoryImpl): PrefectureRepository
}

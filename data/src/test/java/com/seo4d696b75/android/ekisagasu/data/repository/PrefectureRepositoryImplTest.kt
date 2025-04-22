@file:Suppress("NonAsciiCharacters", "RemoveRedundantBackticks")

package com.seo4d696b75.android.ekisagasu.data.repository

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.seo4d696b75.android.ekisagasu.data.station.PrefectureRepositoryImpl
import com.seo4d696b75.android.ekisagasu.domain.dataset.PrefectureRepository
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PrefectureRepositoryImplTest {
    private lateinit var repository: PrefectureRepository

    @Before
    fun setup() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        repository = PrefectureRepositoryImpl(context)
        repository.setData()
    }

    @Test
    fun `不正なcode`() {
        val p1 = repository[0]
        assertThat(p1.name).isEqualTo("unknown")
        val p2 = repository[48]
        assertThat(p2.name).isEqualTo("unknown")
    }

    @Test
    fun `正常系`() {
        val p1 = repository[1]
        assertThat(p1.name).isEqualTo("北海道")
        val p2 = repository[13]
        assertThat(p2.name).isEqualTo("東京都")
    }
}

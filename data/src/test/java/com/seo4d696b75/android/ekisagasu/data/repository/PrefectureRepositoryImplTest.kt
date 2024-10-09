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
        val name1 = repository.getName(0)
        assertThat(name1).isEqualTo("unknown")
        val name2 = repository.getName(48)
        assertThat(name2).isEqualTo("unknown")
    }

    @Test
    fun `正常系`() {
        val name1 = repository.getName(1)
        assertThat(name1).isEqualTo("北海道")
        val name2 = repository.getName(13)
        assertThat(name2).isEqualTo("東京都")
    }
}

@file:Suppress("NonAsciiCharacters", "RemoveRedundantBackticks")

package jp.seo.station.ekisagasu.repository

import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import com.seo4d696b75.android.ekisagasu.data.station.PrefectureRepositoryImpl
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.experimental.runners.Enclosed
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.robolectric.RobolectricTestRunner

@RunWith(Enclosed::class)
open class PrefectureRepositoryImplTest {
    val repository = PrefectureRepositoryImpl()

    @RunWith(JUnit4::class)
    class NotInitTest : PrefectureRepositoryImplTest() {
        @Test
        fun `初期化前`() {
            val name = repository.getName(1)
            assertThat(name).isEqualTo("not-init")
        }
    }

    @ExperimentalCoroutinesApi
    @RunWith(RobolectricTestRunner::class)
    class AlreadyInitTest : PrefectureRepositoryImplTest() {
        init {
            runTest {
                val context = InstrumentationRegistry.getInstrumentation().targetContext
                repository.setData(context)
            }
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
}

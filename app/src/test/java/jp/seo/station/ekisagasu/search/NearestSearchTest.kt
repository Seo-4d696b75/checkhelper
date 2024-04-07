package jp.seo.station.ekisagasu.search

import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.slot
import jp.seo.station.ekisagasu.fakeStations
import jp.seo.station.ekisagasu.fakeTree
import jp.seo.station.ekisagasu.repository.DataRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Test

@ExperimentalCoroutinesApi
class NearestSearchTest {
    private val repository = mockk<DataRepository>()
    private val search: NearestSearch = KdTree(repository)

    private val samples =
        arrayOf(
            LocationTestCase(43.069589, 141.350628, "札幌"),
            LocationTestCase(35.682189, 139.765369, "東京"),
            LocationTestCase(36.702386, 137.212974, "富山"),
            LocationTestCase(35.170488, 136.880076, "名古屋"),
            LocationTestCase(34.703514, 135.494759, "大阪"),
            LocationTestCase(34.704260, 135.496614, "梅田"),
            LocationTestCase(34.704784, 135.499156, "大阪梅田(阪急電鉄)"),
            LocationTestCase(34.701913, 135.499403, "東梅田"),
            LocationTestCase(34.700537, 135.497207, "大阪梅田(阪神電気鉄道)"),
            LocationTestCase(34.700207, 135.494611, "西梅田"),
            LocationTestCase(33.589109, 130.423454, "博多"),
        )

    private val stations by fakeStations
    private val tree by fakeTree

    @Test
    fun testNearestTest() =
        runTest {
            // prepare
            coEvery { repository.getStationKdTree() } answers { tree }
            val codesSlot = slot<List<Int>>()
            coEvery { repository.getStations(capture(codesSlot)) } answers {
                val codes = codesSlot.captured
                codes.map { code ->
                    stations.find { it.code == code } ?: throw NoSuchElementException()
                }
            }

            // test
            for (sample in samples) {
                val result =
                    search.search(
                        sample.lat,
                        sample.lng,
                        k = 1,
                        r = 0.0,
                    )
                assertThat(result.stations.size).isEqualTo(1)
                val s = result.stations[0]
                assertThat(s.name).isEqualTo(sample.stationName)
            }
        }
}

private data class LocationTestCase(
    val lat: Double,
    val lng: Double,
    val stationName: String,
)

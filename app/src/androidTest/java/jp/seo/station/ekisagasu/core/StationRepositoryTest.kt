package jp.seo.station.ekisagasu.core

import android.content.Context
import android.location.Location
import android.os.Looper
import androidx.core.os.HandlerCompat
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import jp.seo.station.ekisagasu.search.KdTree
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException


/**
 * @author Seo-4d696b75
 * @version 2020/12/28.
 */
@RunWith(AndroidJUnit4::class)
class StationRepositoryTest {

    private lateinit var repository: StationRepository
    private lateinit var db: StationDatabase

    @Before
    fun init() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(ctx, StationDatabase::class.java).build()
        val tree = KdTree(db.dao)
        val api =
            getAPIClient("https://raw.githubusercontent.com/Seo-4d696b75/station_database/main/")
        val main = HandlerCompat.createAsync(Looper.getMainLooper())
        repository = StationRepository(db.dao, api, tree, main)
    }

    @Test
    @Throws(Exception::class)
    fun getStationDatabase() {
        runBlocking(Dispatchers.IO) {
            // empty database
            assertThat(repository.lastCheckedVersion, Matchers.nullValue())
            val info = repository.getLatestDataVersion()
            assertThat(info.version, Matchers.greaterThan(0L))
            val version = repository.getDataVersion()
            assertThat(version, Matchers.nullValue())
            assertThat(repository.dataInitialized, Matchers.`is`(false))
            assertThat(repository.lastCheckedVersion, Matchers.notNullValue())
            assertThat(repository.lastCheckedVersion?.version, Matchers.`is`(info.version))

            // update data
            repository.updateData(info, object : StationRepository.UpdateProgressListener {
                override fun onStateChanged(state: String) {
                    assertThat(state, Matchers.not(Matchers.isEmptyString()))
                }

                override fun onProgress(progress: Int) {
                    assertThat(progress, Matchers.greaterThanOrEqualTo(0))
                    assertThat(progress, Matchers.lessThanOrEqualTo(100))
                }

                override fun onComplete(success: Boolean) {
                    assertThat(success, Matchers.`is`(true))
                }

            })
            val current = repository.getDataVersion()
            assertThat(current, Matchers.notNullValue())
            assertThat(current?.version, Matchers.`is`(info.version))
            assertThat(repository.dataInitialized, Matchers.`is`(true))

        }

        val samples = arrayOf(
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
            LocationTestCase(33.589109, 130.423454, "博多")
        )


        runBlocking(Dispatchers.Main) {
            repository.setSearchK(10)
            for (sample in samples) {
                val loc = Location("test-repository")
                loc.latitude = sample.lat
                loc.longitude = sample.lng
                repository.updateNearestStations(loc)
                val s = repository.detectedStation.value
                assertThat(s, Matchers.notNullValue())
                assertThat(s?.station?.name, Matchers.`is`(sample.stationName))
                val n = repository.nearestStation.value
                assertThat(n, Matchers.`is`(s))
            }
            repository.onStopSearch()
            assertThat(repository.nearestStation.value, Matchers.nullValue())
        }
    }

    @After
    @Throws(IOException::class)
    fun onFinish() {
        db.close()
    }
}

data class LocationTestCase(
    val lat: Double,
    val lng: Double,
    val stationName: String
)

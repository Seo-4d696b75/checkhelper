package jp.seo.station.ekisagasu.core

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException
import com.google.common.truth.Truth.assertThat
import jp.seo.station.ekisagasu.search.KdTree


/**
 * @author Seo-4d696b75
 * @version 2020/12/28.
 */
@RunWith(AndroidJUnit4::class)
class StationRepositoryTest {

    private lateinit var repository: StationRepository
    private lateinit var db: StationDatabase

    @Before
    fun init(){
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(ctx, StationDatabase::class.java).build()
        val tree = KdTree(db.dao)
        val api =
            getAPIClient("https://raw.githubusercontent.com/Seo-4d696b75/station_database/extra/")
        repository = StationRepository(db.dao, api, tree)
    }

    @Test
    @Throws(Exception::class)
    fun getStationDatabase() {
        runBlocking(Dispatchers.IO) {
            // empty database
            assertThat(repository.lastCheckedVersion).isNull()
            val info = repository.getLatestDataVersion()
            assertThat(info.version).isGreaterThan(0L)
            val version = repository.getDataVersion()
            assertThat(version).isNull()
            assertThat(repository.dataInitialized).isFalse()
            assertThat(repository.lastCheckedVersion).isNotNull()
            assertThat(repository.lastCheckedVersion?.version).isEqualTo(info.version)

            // update data
            val result = repository.updateData(info.version, info.url)
            assertThat(result).isTrue()
            val current = repository.getDataVersion()
            assertThat(current).isNotNull()
            assertThat(current?.version).isEqualTo(info.version)
            assertThat(repository.dataInitialized).isTrue()

        }
    }

    @After
    @Throws(IOException::class)
    fun onFinish(){
        db.close()
    }
}

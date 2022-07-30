package jp.seo.station.ekisagasu.database

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth
import jp.seo.station.ekisagasu.model.StationData
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.BufferedReader

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
class StationDatabaseTest {

    private lateinit var data: StationData
    private lateinit var stationDB: StationDatabase

    @Before
    fun setup() {
        val json = Json { ignoreUnknownKeys = true }
        val stream = javaClass.classLoader?.getResourceAsStream("data.json")
        val reader = BufferedReader(stream?.reader(Charsets.UTF_8))
        val str = reader.readText()
        data = json.decodeFromString(str)

        val context = InstrumentationRegistry.getInstrumentation().targetContext
        stationDB = Room
            .databaseBuilder(context, StationDatabase::class.java, "station_db")
            .allowMainThreadQueries()
            .build()
    }

    @Test
    fun testStationDB() = runTest {
        val dao = stationDB.dao

        // insert data
        dao.updateData(data)

        // test

        // check data version
        val version = dao.getCurrentDataVersion()
        Truth.assertThat(version?.version).isEqualTo(data.version)

        // get station(s)
        val s1 = data.stations[0]
        val s2 = dao.getStation(s1.code)
        Truth.assertThat(s1).isEqualTo(s2)
        val list1 = data.stations.subList(0, 20).sortedBy { it.code }
        val list2 = dao.getStations(list1.map { it.code })
        Truth.assertThat(list1).isEqualTo(list2)

        // get line(s)
        val l1 = data.lines[0]
        val l2 = dao.getLine(l1.code)
        Truth.assertThat(l1).isEqualTo(l2)
        val lines1 = data.lines.subList(0, 20).sortedBy { it.code }
        val lines2 = dao.getLines(lines1.map { it.code })
        Truth.assertThat(lines1).isEqualTo(lines2)

        // get segment
        val seg1 = data.trees[0]
        val seg2 = dao.getTreeSegment(seg1.name)
        Truth.assertThat(seg1).isEqualTo(seg2)
    }

    @After
    fun teardown() {
        stationDB.close()
    }
}

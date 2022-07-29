package jp.seo.station.ekisagasu.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import jp.seo.station.ekisagasu.model.StationData
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.BufferedReader

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
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

        val context = ApplicationProvider.getApplicationContext<Context>()
        stationDB = Room.inMemoryDatabaseBuilder(context, StationDatabase::class.java).build()
    }

    @Test
    fun testStationDB() = runTest {
        val dao = stationDB.dao

        // insert data
        dao.updateData(data)

        // test

        // check data version
        val version = dao.getCurrentDataVersion()
        assertThat(version?.version).isEqualTo(data.version)

        // get station(s)
        val s1 = data.stations[0]
        val s2 = dao.getStation(s1.code)
        assertThat(s1).isEqualTo(s2)
        val list1 = data.stations.subList(0, 20).sortedBy { it.code }
        val list2 = dao.getStations(list1.map { it.code })
        assertThat(list1).isEqualTo(list2)

        // get line(s)
        val l1 = data.lines[0]
        val l2 = dao.getLine(l1.code)
        assertThat(l1).isEqualTo(l2)
        val lines1 = data.lines.subList(0, 20).sortedBy { it.code }
        val lines2 = dao.getLines(lines1.map { it.code })
        assertThat(lines1).isEqualTo(lines2)

        // get segment
        val seg1 = data.trees[0]
        val seg2 = dao.getTreeSegment(seg1.name)
        assertThat(seg1).isEqualTo(seg2)
    }

    @After
    fun teardown() {
        stationDB.close()
    }
}

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
class UserDatabaseTest {

    private lateinit var userDB: UserDatabase

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        userDB = Room.inMemoryDatabaseBuilder(context, UserDatabase::class.java).build()
    }

    @Test
    fun testUserDB() = runTest {
        val dao = userDB.userDao

        // insert reboot log
        val start = AppLog(AppLog.TYPE_SYSTEM, "start test")
        dao.insertRebootLog(start)
        val since = dao.getLatestID()

        // insert log
        dao.insertLog(AppLog(AppLog.TYPE_SYSTEM, "message1"))
        dao.insertLog(AppLog(AppLog.TYPE_SYSTEM, "message2"))
        dao.insertLog(AppLog(AppLog.TYPE_SYSTEM, "message3"))

        // watch flow
        val rebootList = mutableListOf<List<AppRebootLog>>()
        val logList = mutableListOf<List<AppLog>>()
        val job1 = launch {
            dao.getRebootHistory().toList(rebootList)
        }
        val job2 = launch {
            dao.getLogs(since).toList(logList)
        }

        // test reboot log
        val r = dao.getCurrentReboot()
        assertThat(r.start).isEqualTo(start.timestamp)

        // check flow
        advanceUntilIdle()
        assertThat(rebootList.last().size).isEqualTo(1)
        assertThat(rebootList.last()[0].start).isEqualTo(start.timestamp)
        assertThat(logList.last().size).isGreaterThan(1)
        assertThat(logList.last().last().message).isEqualTo("message3")

        job1.cancel()
        job2.cancel()
    }

    @After
    fun teardown() {
        userDB.close()
    }

}

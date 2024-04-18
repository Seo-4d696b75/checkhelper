package com.seo4d696b75.android.ekisagasu.data.database

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth
import com.seo4d696b75.android.ekisagasu.data.database.station.LineEntity
import com.seo4d696b75.android.ekisagasu.data.database.station.StationDatabase
import com.seo4d696b75.android.ekisagasu.data.database.station.StationEntity
import com.seo4d696b75.android.ekisagasu.data.fakeLatestInfo
import com.seo4d696b75.android.ekisagasu.data.fakeLines
import com.seo4d696b75.android.ekisagasu.data.fakeStations
import com.seo4d696b75.android.ekisagasu.data.fakeTree
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
class StationDatabaseTest {
    private val info by fakeLatestInfo
    private val stations by fakeStations
    private val lines by fakeLines
    private val tree by fakeTree
    private lateinit var stationDB: StationDatabase

    @Before
    fun setup() {
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
        dao.updateData(
            version = info.version,
            stations = stations.map { StationEntity.fromModel(it) },
            lines = lines.map { LineEntity.fromModel(it) },
            tree = tree,
        )

        // test

        // check data version
        val version = dao.getCurrentDataVersion()
        Truth.assertThat(version?.version).isEqualTo(info.version)

        // get station(s)
        val s1 = stations[0]
        val s2 = dao.getStation(s1.code).toModel()
        Truth.assertThat(s1).isEqualTo(s2)
        val list1 = stations.subList(0, 20).sortedBy { it.code }
        val list2 = dao.getStations(list1.map { it.code }).map { it.toModel() }
        Truth.assertThat(list1).isEqualTo(list2)

        // get line(s)
        val l1 = lines[0]
        val l2 = dao.getLine(l1.code).toModel()
        Truth.assertThat(l1).isEqualTo(l2)
        val lines1 = lines.subList(0, 20).sortedBy { it.code }
        val lines2 = dao.getLines(lines1.map { it.code }).map { it.toModel() }
        Truth.assertThat(lines1).isEqualTo(lines2)

        // get segment
        val root = dao.getRootStationNode()
        Truth.assertThat(root.code).isEqualTo(tree.root)
    }

    @After
    fun teardown() {
        stationDB.close()
    }
}

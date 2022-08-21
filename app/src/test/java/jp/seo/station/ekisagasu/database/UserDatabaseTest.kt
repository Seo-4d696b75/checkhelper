package jp.seo.station.ekisagasu.database

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@Ignore("GithubActionで何故か通らん")
@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
class UserDatabaseTest {

    private lateinit var userDB: UserDatabase

    @Before
    fun setup() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        userDB = Room
            .databaseBuilder(context, UserDatabase::class.java, "user_db")
            .allowMainThreadQueries()
            .build()
    }

    @Test
    fun testUserDB() = runTest {
        val dao = userDB.userDao

        // insert reboot log
        val start = AppLog(AppLog.TYPE_SYSTEM, "start test")
        dao.insertRebootLog(start)
        val since = dao.getLatestID()

        // watch flow
        val rebootList = mutableListOf<List<AppRebootLog>>()
        val logList = mutableListOf<List<AppLog>>()
        val job1 = launch {
            dao.getRebootHistory().toList(rebootList)
        }
        val job2 = launch {
            dao.getLogs(since).toList(logList)
        }

        // insert log
        dao.insertLog(AppLog(AppLog.TYPE_SYSTEM, "message1"))
        dao.insertLog(AppLog(AppLog.TYPE_SYSTEM, "message2"))
        dao.insertLog(AppLog(AppLog.TYPE_SYSTEM, "message3"))

        // test reboot log
        val r = dao.getCurrentReboot()
        Truth.assertThat(r.start).isEqualTo(start.timestamp)

        // check flow
        advanceUntilIdle()
        Truth.assertThat(rebootList.last().size).isEqualTo(1)
        Truth.assertThat(rebootList.last()[0].start).isEqualTo(start.timestamp)
        Truth.assertThat(logList.size).isGreaterThan(1)
        Truth.assertThat(logList.last().size).isGreaterThan(1)
        Truth.assertThat(logList.last().last().message).isEqualTo("message3")

        job1.cancel()
        job2.cancel()
    }

    @After
    fun teardown() {
        userDB.close()
    }
}

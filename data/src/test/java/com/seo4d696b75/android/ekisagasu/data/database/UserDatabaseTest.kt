package com.seo4d696b75.android.ekisagasu.data.database

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth
import com.seo4d696b75.android.ekisagasu.data.database.user.AppLogEntity
import com.seo4d696b75.android.ekisagasu.data.database.user.AppRebootEntity
import com.seo4d696b75.android.ekisagasu.data.database.user.UserDatabase
import com.seo4d696b75.android.ekisagasu.domain.log.AppLogType
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

@Ignore("Localだと通るがGithubActionsだと落ちる")
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
        val start = AppLogEntity(AppLogType.System, "start_test")
        dao.insertRebootLog(start)
        val since = dao.getLatestID()

        // watch flow
        val rebootList = mutableListOf<List<AppRebootEntity>>()
        val logList = mutableListOf<List<AppLogEntity>>()
        val job1 = launch {
            dao.getRebootHistory().toList(rebootList)
        }
        val job2 = launch {
            dao.getLogs(since).toList(logList)
        }

        // insert log
        dao.insertLog(AppLogEntity(AppLogType.System, "message1"))
        dao.insertLog(AppLogEntity(AppLogType.System, "message2"))
        dao.insertLog(AppLogEntity(AppLogType.System, "message3"))

        // test reboot log
        val r = dao.getCurrentReboot()
        Truth.assertThat(r.start).isEqualTo(start.timestamp)

        // check flow
        advanceUntilIdle()
        Truth.assertThat(rebootList.last()[0].start).isEqualTo(start.timestamp)
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

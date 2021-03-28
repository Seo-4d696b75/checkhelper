package jp.seo.station.ekisagasu.core

import android.content.Context
import android.util.Log
import androidx.annotation.MainThread
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.switchMap
import jp.seo.station.ekisagasu.R
import jp.seo.station.ekisagasu.utils.TIME_PATTERN_DATETIME
import jp.seo.station.ekisagasu.utils.TIME_PATTERN_ISO8601_EXTEND
import jp.seo.station.ekisagasu.utils.formatTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.util.*

/**
 * @author Seo-4d696b75
 * @version 2020/12/16.
 */
class UserRepository(
    private val dao: UserDao
) {

    companion object {

        private const val KEY_INTERVAL = "interval"
        private const val KEY_RADAR = "radar"
        private const val KEY_VIBRATE = "vibrate"
        private const val KEY_NOTIFY = "notification"
        private const val KEY_FORCE_NOTIFY = "forceNotify"
        private const val KEY_BRIGHTNESS = "brightness"
        private const val KEY_KEEP_NOTIFICATION = "notification_stationary"
        private const val KEY_NOTIFY_PREFECTURE = "notify_prefecture"
        private const val KEY_VIBRATE_METER = "vibrate_meter"
        private const val KEY_VIBRATE_APPROACH = "vibrate_approach"
        private const val KEY_NIGHT_TIMEOUT = "night_mode_timeout"
        private const val KEY_TIMER_POSITION = "timer_position_y"
    }


    val gpsUpdateInterval = MutableLiveData(5)
    val searchK = MutableLiveData<Int>(12)
    val isNotify = MutableLiveData(false)
    val isNotifyForce = MutableLiveData(false)
    val isKeepNotification = MutableLiveData(false)
    val isNotifyPrefecture = MutableLiveData(false)
    val isVibrate = MutableLiveData(false)
    val isVibrateApproach = MutableLiveData(false)
    val vibrateDistance = MutableLiveData(100)
    val nightModeTimeout = MutableLiveData<Int>(0)
    val brightness = MutableLiveData<Int>(128)
    var timerPosition = 0

    private suspend fun loadSetting(context: Context) = withContext(Dispatchers.IO) {
        val reference = context.getSharedPreferences(
            context.getString(R.string.preference_setting_backup),
            Context.MODE_PRIVATE
        )
        gpsUpdateInterval.postValue(reference.getInt(KEY_INTERVAL, 5))
        searchK.postValue(reference.getInt(KEY_RADAR, 12))
        isNotify.postValue(reference.getBoolean(KEY_NOTIFY, true))
        isNotifyForce.postValue(reference.getBoolean(KEY_FORCE_NOTIFY, false))
        isKeepNotification.postValue(reference.getBoolean(KEY_KEEP_NOTIFICATION, false))
        isNotifyPrefecture.postValue(reference.getBoolean(KEY_NOTIFY_PREFECTURE, true))
        isVibrate.postValue(reference.getBoolean(KEY_VIBRATE, false))
        isVibrateApproach.postValue(reference.getBoolean(KEY_VIBRATE_APPROACH, false))
        vibrateDistance.postValue(reference.getInt(KEY_VIBRATE_METER, 300))
        nightModeTimeout.postValue(reference.getInt(KEY_NIGHT_TIMEOUT, 0))
        brightness.postValue(reference.getInt(KEY_BRIGHTNESS, 200))
        timerPosition = reference.getInt(KEY_TIMER_POSITION, -1)
    }


    private fun saveSetting(context: Context) {
        val reference = context.getSharedPreferences(
            context.getString(R.string.preference_setting_backup),
            Context.MODE_PRIVATE
        )
        val editor = reference.edit()
        gpsUpdateInterval.value?.let { editor.putInt(KEY_INTERVAL, it) }
        searchK.value?.let { editor.putInt(KEY_RADAR, it) }
        isNotify.value?.let { editor.putBoolean(KEY_NOTIFY, it) }
        isNotifyForce.value?.let { editor.putBoolean(KEY_FORCE_NOTIFY, it) }
        isKeepNotification.value?.let { editor.putBoolean(KEY_KEEP_NOTIFICATION, it) }
        isNotifyPrefecture.value?.let { editor.putBoolean(KEY_NOTIFY_PREFECTURE, it) }
        isVibrate.value?.let { editor.putBoolean(KEY_VIBRATE, it) }
        isVibrateApproach.value?.let { editor.putBoolean(KEY_VIBRATE_APPROACH, it) }
        vibrateDistance.value?.let { editor.putInt(KEY_VIBRATE_METER, it) }
        nightModeTimeout.value?.let { editor.putInt(KEY_NIGHT_TIMEOUT, it) }
        brightness.value?.let { editor.putInt(KEY_BRIGHTNESS, it) }
        editor.putInt(KEY_TIMER_POSITION, timerPosition)
        editor.apply()
    }

    private val _logFilter = MutableLiveData(LogTarget(null, Long.MAX_VALUE))
    private var _hasError = false

    val history = dao.getRebootHistory()

    val currentLogTarget: LiveData<LogTarget?>
        get() = _logFilter

    suspend fun selectLogsSince(since: AppRebootLog) = withContext(Dispatchers.IO) {
        val until = dao.getNextReboot(since.id)
        _logFilter.postValue(LogTarget(since, since.id, until ?: Long.MAX_VALUE))
    }

    val logs: LiveData<List<AppLog>> = _logFilter.switchMap {
        dao.getLogs(it.since, it.until)
    }

    suspend fun logMessage(message: String) {
        log(AppLog.TYPE_SYSTEM, message)
    }

    suspend fun logError(message: String) {
        log(AppLog.TYPE_SYSTEM, message)
        _hasError = true
    }

    suspend fun logLocation(lat: Double, lng: Double) {
        val mes = String.format("(%.6f,%.6f)", lat, lng)
        log(AppLog.TYPE_LOCATION, mes)
    }

    suspend fun logStation(station: String) {
        log(AppLog.TYPE_STATION, station)
    }

    suspend fun onAppReboot(context: Context) {
        val mes =
            "app started at " + formatTime(TIME_PATTERN_ISO8601_EXTEND, Date())
        val log = AppLog(AppLog.TYPE_SYSTEM, mes)
        dao.insertRebootLog(log)
        val current = dao.getCurrentReboot()
        _logFilter.postValue(LogTarget(current, current.id))
        loadSetting(context)
    }

    private suspend fun log(type: Int, message: String) = withContext(Dispatchers.IO) {
        val log = AppLog(type, message)
        Log.d("AppLog", log.toString())
        dao.insertLog(log)
    }

    @MainThread
    suspend fun onAppFinish(context: Context) {
        saveSetting(context)
        withContext(Dispatchers.IO) {
            if (_hasError) {
                writeErrorLog(
                    context.getString(R.string.app_name),
                    context.getExternalFilesDir(null)
                )
            }
            val log = AppLog(AppLog.TYPE_SYSTEM, "finish app")
            dao.insertLog(log)
            dao.writeFinish(log.timestamp, _hasError)
        }
    }

    private suspend fun writeErrorLog(title: String, dir: File?) {
        val logs = this.logs.value
        withContext(Dispatchers.IO) {
            try {
                val builder = StringBuilder()
                builder.append(title)
                builder.append("\n")
                val time = formatTime(TIME_PATTERN_DATETIME, Date())
                builder.append(
                    String.format(
                        "crash time: %s\n",
                        time
                    )
                )
                logs?.forEach { log ->
                    builder.append(log.toString())
                    builder.append("\n")
                }
                val fileName = String.format("ErrorLog_%s.txt", time)
                File(dir, fileName).writeText(builder.toString(), Charsets.UTF_8)
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

}

data class LogTarget(
    val target: AppRebootLog?,
    val since: Long,
    val until: Long = Long.MAX_VALUE
)

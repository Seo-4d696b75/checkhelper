package jp.seo.station.ekisagasu.core

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.switchMap
import jp.seo.station.ekisagasu.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
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
        private const val KEY_NIGHT = "night_mode"
    }


    //TODO user setting
    val gpsUpdateInterval = MutableLiveData(5)
    val searchK = MutableLiveData<Int>(12)
    val isNotify = MutableLiveData(false)
    val isNotifyForce = MutableLiveData(false)
    val isKeepNotification = MutableLiveData(false)
    val isNotifyPrefecture = MutableLiveData(false)
    val isVibrate = MutableLiveData(false)
    val isVibrateApproach = MutableLiveData(false)
    val vibrateDistance = MutableLiveData(100)
    val nightMode = MutableLiveData(false)
    val brightness = MutableLiveData<Int>(128)

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
        nightMode.postValue(reference.getBoolean(KEY_NIGHT, false))
        brightness.postValue(reference.getInt(KEY_BRIGHTNESS, 200))
    }


    fun saveSetting(context: Context) {
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
        nightMode.value?.let { editor.putBoolean(KEY_NIGHT, it) }
        brightness.value?.let { editor.putInt(KEY_BRIGHTNESS, it) }
        editor.apply()
    }

    private var _oldestID = MutableLiveData<Long>(0L)
    private var _hasError = false

    val hasError: Boolean
        get() = _hasError

    val logs: LiveData<List<AppLog>> by lazy {
        _oldestID.switchMap { dao.getLogs(it) }
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
        //TODO station object
        log(AppLog.TYPE_STATION, station)
    }

    suspend fun onAppReboot(context: Context) {
        val mes =
            "app started at " + SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.US).format(Date())
        val log = AppLog(AppLog.TYPE_SYSTEM, mes)
        dao.insertRebootLog(log)
        val id = dao.getCurrentReboot()
        _oldestID.postValue(id)
        loadSetting(context)
    }

    private suspend fun log(type: Int, message: String) = withContext(Dispatchers.IO) {
        val log = AppLog(type, message)
        Log.d("AppLog", log.toString())
        dao.insertLog(log)
    }

    suspend fun writeErrorLog(title: String, dir: File?) {
        val logs = this.logs.value
        withContext(Dispatchers.IO) {
            try {
                val builder = StringBuilder()
                builder.append(title)
                builder.append("\n")
                val time = SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.US).format(Date())
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

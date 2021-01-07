package jp.seo.station.ekisagasu.core

import android.util.Log
import androidx.annotation.MainThread
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import androidx.lifecycle.switchMap
import com.google.android.gms.common.api.ResolvableApiException
import jp.seo.station.ekisagasu.utils.combine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * @author Seo-4d696b75
 * @version 2020/12/16.
 */
class UserRepository(
    private val dao: UserDao
) {

    //TODO user setting
    val searchK = MutableLiveData<Int>(12)

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

    suspend fun onAppReboot() {
        val mes =
            "app started at " + SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.US).format(Date())
        val log = AppLog(AppLog.TYPE_SYSTEM, mes)
        dao.insertRebootLog(log)
        val id = dao.getCurrentReboot()
        withContext(Dispatchers.Main) {
            _oldestID.value = id
        }
    }

    private suspend fun log(type: Int, message: String) {
        val log = AppLog(type, message)
        Log.d("AppLog", log.toString())
        dao.insertLog(log)
    }

    fun writeErrorLog(title: String, dir: File?) {
        val logs = this.logs.value
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
    }

    private val _apiException = MutableLiveData<ResolvableApiException?>(null)

    val apiException: LiveData<ResolvableApiException?>
        get() = _apiException

    @MainThread
    fun onApiException(e: ResolvableApiException){
        _apiException.value = e
    }

    @MainThread
    fun onResolvedAPIException(){
        _apiException.value = null
    }
}

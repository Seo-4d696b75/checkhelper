package jp.seo.station.ekisagasu.viewmodel

import android.app.Activity
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.*
import jp.seo.station.ekisagasu.R
import jp.seo.station.ekisagasu.core.*
import jp.seo.station.ekisagasu.ui.*
import jp.seo.station.ekisagasu.utils.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedWriter
import java.io.IOException
import java.io.OutputStreamWriter
import java.util.*
import kotlin.collections.ArrayList

/**
 * Activityスコープで共有するViewModel
 *
 * [MainViewModel]とは異なりUI更新情報ではなくデータやパーミッションなどの情報を共有する
 * @author Seo-4d696b75
 * @version 2021/01/13.
 */
class ActivityViewModel(
    context: Context,
    private val stationRepository: StationRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    companion object {
        fun getInstance(
            owner: ViewModelStoreOwner,
            context: Context,
            stationRepository: StationRepository,
            userRepository: UserRepository
        ): ActivityViewModel {
            val factory = getViewModelFactory {
                ActivityViewModel(
                    context,
                    stationRepository,
                    userRepository
                )
            }
            return ViewModelProvider(owner, factory).get(ActivityViewModel::class.java)
        }

        fun getDialog(type: String): DialogFragment? {
            return when (type) {
                DataDialog.DIALOG_UPDATE -> DataUpdateDialog()
                DataDialog.DIALOG_INIT -> DataCheckDialog()
                DataDialog.DIALOG_LATEST -> DataCheckDialog()
                LineDialog.DIALOG_SELECT_NAVIGATION -> LineDialog()
                LineDialog.DIALOG_SELECT_CURRENT -> LineDialog()
                AppHistoryDialog.DIALOG_SELECT_HISTORY -> AppHistoryDialog()
                else -> null
            }
        }

        val LOG_FILTERS = arrayOf(
            LogFilter(AppLog.FILTER_ALL, "ALL"),
            LogFilter(AppLog.TYPE_SYSTEM, "System"),
            LogFilter(AppLog.FILTER_GEO, "Geo"),
            LogFilter(AppLog.TYPE_STATION, "Station")
        )
    }

    private val messageAbortInit = context.getString(R.string.message_abort_init_data)
    private val messageSuccessUpdate = context.getString(R.string.message_success_data_update)

    val requestFinish = UnitLiveEvent(true)

    private var hasPermissionChecked = false
    private var hasVersionChecked = false

    /**
     * check whether connected service is ready for use.
     * (1) check data version
     * (2) check data initialized
     */
    fun checkData() {

        // check data version
        if (!hasVersionChecked || !stationRepository.dataInitialized) {
            hasVersionChecked = true

            viewModelScope.launch {

                val info = stationRepository.getDataVersion()
                val latest = stationRepository.getLatestDataVersion(false)

                if (info == null) {
                    targetInfo = latest
                    requestDialog(DataDialog.DIALOG_INIT)
                } else {
                    userRepository.logMessage(String.format("data found version:${info.version}"))
                    if (info.version < latest.version) {
                        targetInfo = latest
                        requestDialog(DataDialog.DIALOG_LATEST)
                    }
                }

            }
        }
    }

    fun checkPermission(activity: AppCompatActivity): Boolean {
        if (hasPermissionChecked) return true


        viewModelScope.launch {
            userRepository.logMessage("all permission checked")
        }
        hasPermissionChecked = true
        return true
    }

    val requestToast = LiveEvent<String>()

    fun handleDialogResult(
        type: String,
        info: DataLatestInfo?,
        result: Boolean
    ) {
        when (type) {
            DataDialog.DIALOG_INIT -> {
                if (result) {
                    requestDialog(DataDialog.DIALOG_UPDATE)
                } else {
                    requestToast.call(messageAbortInit)
                    requestFinish.call()
                }
            }
            DataDialog.DIALOG_LATEST -> {
                if (result) {
                    requestDialog(DataDialog.DIALOG_UPDATE)
                }
            }
            DataDialog.DIALOG_UPDATE -> {
                if (result && info != null) {
                    val mes = String.format(
                        "%s\nversion: %d",
                        messageSuccessUpdate,
                        info.version
                    )
                    requestToast.call(mes)
                }
            }
            else -> {
                Log.d("DialogResult", "unknown type: $type")
            }
        }
    }

    fun requestDialog(type: String) {
        requestDialog.call(type)
        dialogType = type
    }


    val requestDialog = LiveEvent<String>()
    var dialogType: String = "none"
    var targetInfo: DataLatestInfo? = null

    private val _updateState = MutableLiveData<String>("")
    private val _updateProgress = MutableLiveData<Int>(0)

    val updateState: LiveData<String>
        get() = _updateState

    val updateProgress: LiveData<Int>
        get() = _updateProgress

    fun updateStationData(info: DataLatestInfo, callback: (Boolean) -> Unit) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                stationRepository.updateData(
                    info,
                    object : StationRepository.UpdateProgressListener {
                        override fun onStateChanged(state: String) {
                            _updateState.value = state
                        }

                        override fun onProgress(progress: Int) {
                            _updateProgress.value = progress
                        }

                        override fun onComplete(success: Boolean) {
                            callback(success)
                        }

                    })
            }
        }
    }

    fun checkLatestData(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            val latest = stationRepository.getLatestDataVersion(true)
            val current = stationRepository.getDataVersion()
            if (current == null || latest.version > current.version) {
                targetInfo = latest
                requestDialog(DataDialog.DIALOG_LATEST)
            } else {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        context,
                        context.getString(R.string.data_already_latest),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    data class LogFilter(
        val filter: Int,
        val name: String,
    )


    private var _filter = MutableLiveData(LOG_FILTERS[0])

    fun setFilter(value: LogFilter) {
        _filter.value = value
    }

    fun setLogTarget(target: AppRebootLog) = viewModelScope.launch {
        userRepository.selectLogsSince(target)
    }

    val histories = userRepository.history
    val logTarget = userRepository.currentLogTarget

    val logs: LiveData<List<AppLog>> = combineLiveData(
        ArrayList(),
        _filter,
        userRepository.logs
    ) { filter, logs ->
        logs.filter { (it.type and filter.filter) > 0 }
    }


    fun writeLog(title: String, activity: Activity) {
        val builder = StringBuilder()
        val time = Date()
        _filter.value?.let { type ->
            logs.value?.let { list ->
                val fileName = String.format(
                    Locale.US, "%s_%sLog_%s.txt", title, type.name, formatTime(
                        TIME_PATTERN_DATETIME_FILE, time
                    )
                )
                builder.append(title)
                builder.append("\nlog type : ")
                builder.append(type.name)
                builder.append("\nwritten time : ")
                builder.append(formatTime(TIME_PATTERN_DATETIME, time))
                for (log in list) {
                    builder.append("\n")
                    builder.append(formatTime(TIME_PATTERN_MILLI_SEC, log.timestamp))
                    builder.append(" ")
                    builder.append(log.message)
                }
                targetFileContent = builder.toString()
                val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    this.type = "text/plain"
                    putExtra(Intent.EXTRA_TITLE, fileName)
                }
                activity.startActivityForResult(intent, MainActivity.WRITE_EXTERNAL_FILE)
            }
        }
    }

    private var targetFileContent: String? = null

    fun writeFile(uri: Uri, resolver: ContentResolver) = viewModelScope.launch(Dispatchers.IO) {
        targetFileContent?.let { content ->
            try {
                resolver.openOutputStream(uri).use {
                    val writer = BufferedWriter(OutputStreamWriter(it, Charsets.UTF_8))
                    writer.write(content)
                    writer.close()
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
            targetFileContent = null
        }
    }

}

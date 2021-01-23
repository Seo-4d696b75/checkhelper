package jp.seo.station.ekisagasu.viewmodel

import android.Manifest
import android.app.Activity
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.*
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import jp.seo.station.ekisagasu.R
import jp.seo.station.ekisagasu.core.AppLog
import jp.seo.station.ekisagasu.core.DataLatestInfo
import jp.seo.station.ekisagasu.core.StationRepository
import jp.seo.station.ekisagasu.core.UserRepository
import jp.seo.station.ekisagasu.ui.DataCheckDialog
import jp.seo.station.ekisagasu.ui.DataDialog
import jp.seo.station.ekisagasu.ui.DataUpdateDialog
import jp.seo.station.ekisagasu.ui.MainActivity
import jp.seo.station.ekisagasu.utils.combineLiveData
import jp.seo.station.ekisagasu.utils.getViewModelFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedWriter
import java.io.IOException
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
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
                else -> null
            }
        }
    }

    private val messageAbortInit = context.getString(R.string.message_abort_init_data)
    private val messageSuccessUpdate = context.getString(R.string.message_success_data_update)

    val requestFinish = MutableLiveData<Boolean>(false)

    private var hasPermissionChecked = false
    private var hasInitialized = false
    private var hasVersionChecked = false

    /**
     * initialize permission and data
     *
     * @param block once executed after initialized
     */
    fun initialize(activity: AppCompatActivity, block: () -> Unit) {
        // TODO check notification channel
        if (checkPermission(activity)) {
            checkData(block)
        }
    }

    /**
     * check whether connected service is ready for use.
     * (1) check data version
     * (2) check data initialized
     */
    private fun checkData(block: () -> Unit) {

        // check data version
        if (!hasVersionChecked) {
            hasVersionChecked = true

            viewModelScope.launch {

                val info = stationRepository.getDataVersion()
                val latest = stationRepository.getLatestDataVersion(false)

                if (info == null) {
                    targetInfo = latest
                    requestDialog(DataDialog.DIALOG_INIT)
                } else {
                    withContext(Dispatchers.IO) {
                        userRepository.logMessage(String.format("data found version:${info.version}"))
                    }
                    if (info.version < latest.version) {
                        targetInfo = latest
                        requestDialog(DataDialog.DIALOG_LATEST)
                    }
                    if (!hasInitialized) {
                        hasInitialized = true
                        block()
                    }
                }

            }
        }
        if (stationRepository.dataInitialized && !hasInitialized) {
            hasInitialized = true
            block()
        }
    }

    private fun checkPermission(activity: AppCompatActivity): Boolean {
        if (hasPermissionChecked) return true

        // Runtime Permission required API level >= 23
        if (!Settings.canDrawOverlays(activity)) {
            Toast.makeText(
                activity.applicationContext,
                "Need \"DrawOverlay\" Permission",
                Toast.LENGTH_SHORT
            ).show()
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${activity.packageName}")
            )
            activity.startActivityForResult(intent, MainActivity.PERMISSION_REQUEST_OVERLAY)
            return false
        } else if (
            ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
            || ContextCompat.checkSelfPermission(
                activity,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ),
                MainActivity.PERMISSION_REQUEST
            )
            return false
        }

        val code = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(activity)
        if (code != ConnectionResult.SUCCESS) {
            GoogleApiAvailability.getInstance().getErrorDialog(activity, code, 0).show()
            return false
        }


        viewModelScope.launch(Dispatchers.IO) {
            userRepository.logMessage("all permission checked")
        }
        hasPermissionChecked = true
        return true
    }

    private val toastText = MutableLiveData<String?>(null)
    val requestedToastTest: LiveData<String?> = toastText
    private fun requestToast(text: String) {
        toastText.value = text
    }

    fun clearToastText() {
        toastText.value = null
    }

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
                    requestToast(messageAbortInit)
                    requestFinish.value = true
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
                    requestToast(mes)
                }
            }
            else -> {
                Log.d("DialogResult", "unknown type: $type")
            }
        }
    }

    private val _requestDialog = MutableLiveData<String?>(null)
    val requestedDialog: LiveData<String?> = _requestDialog
    fun requestDialog(type: String) {
        _requestDialog.value = type
        dialogType = type
    }

    fun clearRequestDialog() {
        _requestDialog.value = null
    }

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

    fun checkLatestData() {
        viewModelScope.launch(Dispatchers.IO) {
            val latest = stationRepository.getLatestDataVersion(true)
            val current = stationRepository.getDataVersion()
            if (current == null || latest.version > current.version) {
                targetInfo = latest
                requestDialog(DataDialog.DIALOG_LATEST)
            }
        }
    }


    private var _filter: MutableLiveData<Int> = MutableLiveData(AppLog.FILTER_ALL)

    fun setFilter(value: Int) {
        _filter.value = value
    }

    val logs: LiveData<List<AppLog>> = combineLiveData(
        ArrayList(),
        _filter,
        userRepository.logs
    ) { filter, logs ->
        logs.filter { (it.type and filter) > 0 }
    }


    fun writeLog(title: String, activity: Activity) {
        val builder = StringBuilder()
        val time = SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.US).format(Date())
        _filter.value?.let { type ->
            logs.value?.let { list ->
                val fileName = String.format(Locale.US, "%sLog_%s.txt", type, time)
                builder.append(title)
                builder.append("\nlog type : ")
                builder.append(type)
                builder.append("\ntime : ")
                builder.append(time)
                for (log in list) {
                    builder.append("\n")
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

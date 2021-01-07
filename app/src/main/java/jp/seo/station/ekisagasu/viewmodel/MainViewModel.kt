package jp.seo.station.ekisagasu.viewmodel


import android.Manifest
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.*
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.common.api.ResolvableApiException
import jp.seo.station.ekisagasu.R
import jp.seo.station.ekisagasu.Station
import jp.seo.station.ekisagasu.core.DataLatestInfo
import jp.seo.station.ekisagasu.core.StationService
import jp.seo.station.ekisagasu.core.UserRepository
import jp.seo.station.ekisagasu.ui.DataCheckDialog
import jp.seo.station.ekisagasu.ui.DataDialog
import jp.seo.station.ekisagasu.ui.DataUpdateDialog
import jp.seo.station.ekisagasu.ui.MainActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * @author Seo-4d696b75
 * @version 2020/12/16.
 */
class MainViewModel : ViewModel() {

    private var hasPermissionChecked = false
    private var hasRequestService = false
    private var hasServiceChecked = false
    var hasInitializedUI = false

    fun startService(activity: AppCompatActivity, connection: ServiceConnection) {
        if (!hasPermissionChecked) {
            checkPermission(activity)
        }
        if (!hasRequestService) {
            val intent = Intent(activity, StationService::class.java)
            activity.bindService(intent, connection, Context.BIND_AUTO_CREATE)
            hasRequestService = true
        }
    }

    fun checkService(
        service: StationService?,
        activity: AppCompatActivity,
        initializer: (StationService) -> Unit
    ) {
        if (hasServiceChecked) return
        service?.let {
            viewModelScope.launch {
                withContext(Dispatchers.IO) {

                    // TODO check notification channel
                    val info = it.stationRepository.getDataVersion()
                    val latest = it.stationRepository.getLatestDataVersion()

                    withContext(Dispatchers.Main) {
                        userRepository.value = it.userRepository

                        if (info == null) {
                            val dialog = DataCheckDialog.getInstance(latest, true)
                            dialog.show(activity.supportFragmentManager, DataDialog.DIALOG_INIT)
                        } else {
                            if (info.version < latest.version) {
                                val dialog = DataCheckDialog.getInstance(latest, false)
                                dialog.show(
                                    activity.supportFragmentManager,
                                    DataDialog.DIALOG_LATEST
                                )
                            }
                            if (!hasServiceChecked) {
                                hasServiceChecked = true
                                initializer(it)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun checkPermission(activity: AppCompatActivity) {

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
            return
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
            return
        }

        val code = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(activity)
        if (code != ConnectionResult.SUCCESS) {
            GoogleApiAvailability.getInstance().getErrorDialog(activity, code, 0).show()
            return
        }

        val intent = Intent(activity, StationService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            activity.startForegroundService(intent)
        } else {
            activity.startService(intent)
        }

        hasPermissionChecked = true
    }

    private var userRepository = MutableLiveData<UserRepository?>(null)

    val apiException: LiveData<ResolvableApiException?> =
        userRepository.switchMap { it?.apiException ?: MutableLiveData(null) }

    fun handleDialogButton(
        tag: String,
        info: DataLatestInfo,
        which: Int,
        activity: AppCompatActivity
    ) {
        when (tag) {
            DataDialog.DIALOG_INIT -> {
                if (which == DialogInterface.BUTTON_POSITIVE) {
                    val dialog = DataUpdateDialog.getInstance(info)
                    dialog.show(activity.supportFragmentManager, DataDialog.DIALOG_UPDATE)
                } else if (which == DialogInterface.BUTTON_NEGATIVE) {
                    Toast.makeText(
                        activity,
                        activity.getString(R.string.message_abort_init_data),
                        Toast.LENGTH_SHORT
                    ).show()
                    activity.finish()
                }
            }
            DataDialog.DIALOG_LATEST -> {
                if (which == DialogInterface.BUTTON_POSITIVE) {
                    val dialog = DataUpdateDialog.getInstance(info)
                    dialog.show(activity.supportFragmentManager, DataDialog.DIALOG_UPDATE)
                }
            }
            DataDialog.DIALOG_UPDATE -> {
                if (which == DialogInterface.BUTTON_POSITIVE) {
                    val mes = String.format(
                        "%s\nversion: %d",
                        activity.getString(R.string.message_success_data_update),
                        info.version
                    )
                    Toast.makeText(activity, mes, Toast.LENGTH_SHORT).show()
                }
            }
            else -> {
                Log.d("Dialog", "unknown tag: $tag")
            }
        }
    }

}

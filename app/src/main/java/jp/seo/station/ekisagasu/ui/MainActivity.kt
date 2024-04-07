package jp.seo.station.ekisagasu.ui

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import dagger.hilt.android.AndroidEntryPoint
import jp.seo.station.ekisagasu.R
import jp.seo.station.ekisagasu.model.AppMessage
import jp.seo.station.ekisagasu.service.StationService
import jp.seo.station.ekisagasu.ui.dialog.ConfirmDataUpdateDialogDirections
import jp.seo.station.ekisagasu.ui.dialog.DataUpdateDialogDirections
import jp.seo.station.ekisagasu.ui.dialog.DataUpdateType
import jp.seo.station.ekisagasu.ui.dialog.LineDialogDirections
import jp.seo.station.ekisagasu.ui.dialog.LineDialogType
import jp.seo.station.ekisagasu.ui.log.LogViewModel
import jp.seo.station.ekisagasu.utils.navigateWhenDialogClosed
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import timber.log.Timber

/**
 * @author Seo-4d696b75
 * @version 2020/12/16.
 */
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        // TODO activityの再生成が失敗するので暫定的に初期状態から
        super.onCreate(null)
        setContentView(R.layout.main_activity)

        // handle message
        viewModel.message
            .flowWithLifecycle(lifecycle)
            .onEach { message ->
                when (message) {
                    is AppMessage.ResolvableException -> {
                        resolvableApiLauncher.launch(
                            IntentSenderRequest.Builder(message.exception.resolution).build(),
                        )
                    }
                    is AppMessage.StartActivityForResult -> {
                        when (message.code) {
                            LogViewModel.REQUEST_CODE_LOG_FILE_URI -> {
                                requestLogFileUriLauncher.launch(message.intent)
                            }
                            else -> {
                                Timber.tag("MainActivity").w("unknown request: $message")
                            }
                        }
                    }
                    is AppMessage.FinishApp -> {
                        finish()
                    }
                    is AppMessage.RequestDataUpdate -> {
                        // ユーザの確認を経てから実行
                        if (message.confirmed) {
                            val action =
                                DataUpdateDialogDirections.actionGlobalDataUpdateDialog(
                                    info = message.info,
                                    type = message.type,
                                )
                            findNavController(R.id.main_nav_host).navigateWhenDialogClosed(
                                action,
                                R.id.confirmDataUpdateDialog,
                                lifecycle,
                            )
                        } else {
                            val action =
                                ConfirmDataUpdateDialogDirections.actionGlobalConfirmDataUpdateDialog(
                                    info = message.info,
                                    type = message.type,
                                )
                            findNavController(R.id.main_nav_host).navigate(action)
                        }
                    }
                    is AppMessage.DataUpdateResult -> {
                        val resId =
                            if (message.success) {
                                R.string.message_success_data_update
                            } else if (message.type == DataUpdateType.Init) {
                                // データの初期化に失敗・これ以上の続行不可能
                                viewModel.requestAppFinish()
                                R.string.message_fail_data_initialize
                            } else {
                                R.string.message_fail_data_update
                            }
                        Toast.makeText(this, resId, Toast.LENGTH_LONG).show()
                    }
                    is AppMessage.CheckLatestVersionFailure -> {
                        Toast.makeText(
                            this,
                            R.string.message_network_failure,
                            Toast.LENGTH_LONG,
                        ).show()
                    }
                    is AppMessage.VersionUpToDate -> {
                        Toast.makeText(
                            this,
                            R.string.message_version_up_to_date,
                            Toast.LENGTH_LONG,
                        ).show()
                    }
                    else -> {}
                }
            }
            .launchIn(lifecycleScope)
    }

    override fun onResume() {
        super.onResume()
        checkPermission()
        if (viewModel.hasPermissionChecked) {
            startService()
            viewModel.checkData()
        }

        // handle intent
        intent?.let {
            if (it.getBooleanExtra(INTENT_KEY_SELECT_NAVIGATION, false)) {
                it.putExtra(INTENT_KEY_SELECT_NAVIGATION, false)
                val action = LineDialogDirections.actionGlobalLineDialog(LineDialogType.Navigation)
                findNavController(R.id.main_nav_host).navigate(action)
            }
        }
    }

    companion object {
        const val INTENT_KEY_SELECT_NAVIGATION = "select_navigation_line"
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun startService() {
        if (!viewModel.isServiceRunning) {
            val intent = Intent(this, StationService::class.java)
            startForegroundService(intent)
            viewModel.isServiceRunning = true
        }
    }

    private fun checkPermission() {
        if (viewModel.hasPermissionChecked) return
        // Runtime Permission required API level >= 23
        // TODO permissionの必要をユーザ側に説明するUI
        if (!Settings.canDrawOverlays(applicationContext)) {
            Toast.makeText(
                applicationContext,
                "Need \"DrawOverlay\" Permission",
                Toast.LENGTH_SHORT,
            ).show()
            val intent =
                Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:${applicationContext.packageName}"),
                )
            overlayPermissionLauncher.launch(intent)
            return
        }
        if (
            ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.ACCESS_FINE_LOCATION,
            )
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            return
        }

        val code = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this)
        if (code != ConnectionResult.SUCCESS) {
            GoogleApiAvailability.getInstance().getErrorDialog(this, code, 0)?.show()
            return
        }

        viewModel.hasPermissionChecked = true
    }

    private val overlayPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult(),
        ) {
            if (it.resultCode == Activity.RESULT_OK) {
                Timber.tag("Permission").i(getString(R.string.message_permission_overlay_granted))
            } else {
                Timber.tag("Permission").i(getString(R.string.message_permission_overlay_denied))
                if (!Settings.canDrawOverlays(this)) {
                    Toast.makeText(
                        applicationContext,
                        getString(R.string.message_permission_denied),
                        Toast.LENGTH_SHORT,
                    ).show()
                    finish()
                }
            }
        }

    private val resolvableApiLauncher =
        registerForActivityResult(
            ActivityResultContracts.StartIntentSenderForResult(),
        ) {
            if (it.resultCode == Activity.RESULT_OK) {
                Timber.tag("ResolvableAPIException").i(
                    getString(R.string.message_resolvable_exception_success, it.toString()),
                )
            } else {
                Timber.tag("ResolvableAPIException").i(
                    getString(R.string.message_resolvable_exception_failure, it.toString()),
                )
            }
        }

    private val requestPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission(),
        ) { }

    private val requestLogFileUriLauncher =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult(),
        ) { result ->
            viewModel.onActivityResultResolved(
                LogViewModel.REQUEST_CODE_LOG_FILE_URI,
                result.resultCode,
                result.data,
            )
        }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_setting -> {
                findNavController(R.id.main_nav_host).navigate(R.id.action_mainFragment_to_settingFragment)
            }
            R.id.menu_log -> {
                findNavController(R.id.main_nav_host).navigate(R.id.action_mainFragment_to_logFragment)
            }
            else -> {
                Timber.tag("Menu").w("unknown id:${item.itemId}")
            }
        }
        return super.onOptionsItemSelected(item)
    }
}

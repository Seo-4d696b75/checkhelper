package jp.seo.station.ekisagasu.ui

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
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
import jp.seo.station.ekisagasu.ui.dialog.LineDialogDirections
import jp.seo.station.ekisagasu.ui.dialog.LineDialogType
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/**
 * @author Seo-4d696b75
 * @version 2020/12/16.
 */
@ExperimentalCoroutinesApi
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        // TODO activityの再生成が失敗するので暫定的に初期状態から
        super.onCreate(null)
        setContentView(R.layout.main_activity)

        // try to resolve API exception if any
        viewModel.message
            .flowWithLifecycle(lifecycle)
            .onEach { message ->
                when (message) {
                    is AppMessage.ResolvableException -> {
                        resolvableApiLauncher.launch(
                            IntentSenderRequest.Builder(message.exception.resolution).build()
                        )
                    }
                    is AppMessage.StartActivityForResult -> {
                        val launcher = registerForActivityResult(
                            ActivityResultContracts.StartActivityForResult()
                        ) { result ->
                            viewModel.onActivityResultResolved(
                                message.code,
                                result.resultCode,
                                result.data
                            )
                        }
                        launcher.launch(message.intent)
                    }
                    is AppMessage.FinishApp -> {
                        finish()
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
                Toast.LENGTH_SHORT
            ).show()
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${applicationContext.packageName}")
            )
            overlayPermissionLauncher.launch(intent)
            return
        }
        if (
            ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            return
        }

        if (
            ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            return
        }

        val code = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this)
        if (code != ConnectionResult.SUCCESS) {
            GoogleApiAvailability.getInstance().getErrorDialog(this, code, 0)?.show()
            return
        }

        viewModel.hasPermissionChecked = true
    }

    private val overlayPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (it.resultCode == Activity.RESULT_OK) {
            Log.d("Overlay", "permission_request_overlay")
            if (Build.VERSION.SDK_INT == Build.VERSION_CODES.O) {
                Toast.makeText(applicationContext, "Please reboot app", Toast.LENGTH_SHORT).show()
            }
        } else {
            if (!Settings.canDrawOverlays(this)) {
                Toast.makeText(
                    applicationContext,
                    "overlay permission not granted",
                    Toast.LENGTH_SHORT
                ).show()
                finish()
            }
        }
    }

    private val resolvableApiLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) {
        if (it.resultCode == Activity.RESULT_OK) {
            Log.d("ResolvableAPI", "resolved")
        } else {
            Log.d("ResolvableAPI", "fail")
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        if (!it) {
            Toast.makeText(
                applicationContext,
                "permission not granted",
                Toast.LENGTH_SHORT
            ).show()
            finish()
        }
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
                Log.e("Menu", "unknown id:${item.itemId}")
            }
        }
        return super.onOptionsItemSelected(item)
    }

}

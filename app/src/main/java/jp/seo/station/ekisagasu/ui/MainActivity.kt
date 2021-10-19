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
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelStore
import androidx.navigation.findNavController
import dagger.hilt.android.AndroidEntryPoint
import jp.seo.station.ekisagasu.R
import jp.seo.station.ekisagasu.core.GPSClient
import jp.seo.station.ekisagasu.core.NavigationRepository
import jp.seo.station.ekisagasu.core.StationRepository
import jp.seo.station.ekisagasu.core.UserRepository
import jp.seo.station.ekisagasu.viewmodel.ActivityViewModel
import jp.seo.station.ekisagasu.viewmodel.ApplicationViewModel
import javax.inject.Inject

/**
 * @author Seo-4d696b75
 * @version 2020/12/16.
 */
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)

        appViewModel.isActivityAlive = true

        // try to resolve API exception if any
        appViewModel.apiException.observe(this) {
            resolvableApiLauncher.launch(
                IntentSenderRequest.Builder(it.resolution).build()
            )
        }

        // finish activity if requested
        viewModel.requestFinish.observe(this) {
            appViewModel.finish()
        }
        appViewModel.requestFinishActivity.observe(this) {
            finish()
        }

        viewModel.requestDialog.observe(this) { type ->
            ActivityViewModel.getDialog(type)?.show(supportFragmentManager, type)
        }
        viewModel.requestToast.observe(this) {
            Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()
        appViewModel.startService(this)
        viewModel.checkData()
        checkPermission()

        intent?.let {
            if (it.getBooleanExtra(INTENT_KEY_SELECT_NAVIGATION, false)) {
                it.putExtra(INTENT_KEY_SELECT_NAVIGATION, false)
                viewModel.requestDialog(LineDialog.DIALOG_SELECT_NAVIGATION)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        appViewModel.isActivityAlive = false
    }

    @Inject
    lateinit var singletonStore: ViewModelStore

    @Inject
    lateinit var stationRepository: StationRepository

    @Inject
    lateinit var userRepository: UserRepository

    @Inject
    lateinit var gpsClient: GPSClient

    @Inject
    lateinit var navigator: NavigationRepository

    private val viewModel: ActivityViewModel by lazy {
        // ActivityScoped
        ActivityViewModel.getInstance(this, this, stationRepository, userRepository)
    }

    private val appViewModel: ApplicationViewModel by lazy {
        // SingletonScoped
        ApplicationViewModel.getInstance(
            { singletonStore },
            stationRepository,
            userRepository,
            gpsClient,
            navigator
        )
    }

    companion object {
        const val PERMISSION_REQUEST_OVERLAY = 3900
        const val PERMISSION_REQUEST = 3901
        const val WRITE_EXTERNAL_FILE = 3903
        const val INTENT_KEY_SELECT_NAVIGATION = "select_navigation_line"
    }

    private fun checkPermission() {
        // Runtime Permission required API level >= 23
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
            || ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ),
                PERMISSION_REQUEST
            )
            return
        }

        viewModel.checkPermission(this)
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == WRITE_EXTERNAL_FILE) {
            if (resultCode == Activity.RESULT_OK) {
                data?.data?.let { viewModel.writeFile(it, contentResolver) }
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST) {
            if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(
                    applicationContext,
                    "location permission not granted",
                    Toast.LENGTH_SHORT
                ).show()
                finish()
            }
            if (grantResults[1] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(
                    applicationContext,
                    "storage permission not granted",
                    Toast.LENGTH_SHORT
                ).show()
                finish()
            }

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

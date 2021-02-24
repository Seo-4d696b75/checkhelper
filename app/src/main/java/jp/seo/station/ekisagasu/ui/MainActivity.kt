package jp.seo.station.ekisagasu.ui

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.content.IntentSender
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelStore
import androidx.navigation.findNavController
import dagger.hilt.android.AndroidEntryPoint
import jp.seo.station.ekisagasu.R
import jp.seo.station.ekisagasu.core.StationRepository
import jp.seo.station.ekisagasu.core.StationService
import jp.seo.station.ekisagasu.core.UserRepository
import jp.seo.station.ekisagasu.viewmodel.ActivityViewModel
import jp.seo.station.ekisagasu.viewmodel.ApplicationViewModel
import javax.inject.Inject

/**
 * @author Seo-4d696b75
 * @version 2020/12/16.
 */
@AndroidEntryPoint
class MainActivity : AppCompatActivity(), ServiceConnection {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)

        appViewModel.isActivityAlive = true

        // try to resolve API exception if any
        appViewModel.apiException.observe(this) {
            try {
                it?.startResolutionForResult(this, RESOLVE_API_EXCEPTION)
            } catch (e: IntentSender.SendIntentException) {
                Log.e(e.javaClass.name, e.message ?: "fail to resolve")
            }
        }

        // finish activity if requested
        viewModel.requestFinish.observe(this) {
            appViewModel.finish()
        }
        appViewModel.requestFinishActivity.observeForever {
            unbindService(this)
            stopService(Intent(this, StationService::class.java))
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
        viewModel.checkPermission(this)
        viewModel.checkData()
        appViewModel.startService(this)
        viewModel.bindService(this, this)
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
        )
    }

    companion object {
        const val PERMISSION_REQUEST_OVERLAY = 3900
        const val PERMISSION_REQUEST = 3901
        const val RESOLVE_API_EXCEPTION = 3902
        const val WRITE_EXTERNAL_FILE = 3903
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PERMISSION_REQUEST_OVERLAY) {
            Log.d("ActivityResult", "permission_request_overlay")
            if (Build.VERSION.SDK_INT == Build.VERSION_CODES.O) {
                Toast.makeText(applicationContext, "Please reboot app", Toast.LENGTH_SHORT).show()
            }
            if (!Settings.canDrawOverlays(this)) {
                Toast.makeText(
                    applicationContext,
                    "overlay permission not granted",
                    Toast.LENGTH_SHORT
                ).show()
                finish()
            }
        } else if (requestCode == RESOLVE_API_EXCEPTION) {
            Log.d("ActivityResult", "resolve_api_exception")
        } else if (requestCode == WRITE_EXTERNAL_FILE) {
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

    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
        Log.d("Activity", "service connected")
    }

    override fun onServiceDisconnected(name: ComponentName?) {
        Log.d("Activity", "service disconnected")
    }

}

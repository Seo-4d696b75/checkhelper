package jp.seo.station.ekisagasu.ui

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
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import jp.seo.station.ekisagasu.R
import jp.seo.station.ekisagasu.core.DataLatestInfo
import jp.seo.station.ekisagasu.core.StationService
import jp.seo.station.ekisagasu.utils.ServiceGetter
import jp.seo.station.ekisagasu.utils.getViewModelFactory
import jp.seo.station.ekisagasu.viewmodel.MainViewModel

/**
 * @author Seo-4d696b75
 * @version 2020/12/16.
 */
class MainActivity : AppCompatActivity(), ServiceConnection, DataDialog.OnClickListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)

        lifecycle.addObserver(service)

        findViewById<Button>(R.id.button_stop).setOnClickListener {
            finish()
        }
        viewModel.apiException.observe(this) {
            try{
                it?.startResolutionForResult(this, RESOLVE_API_EXCEPTION)
            } catch (e: IntentSender.SendIntentException){
                service.value?.error("Resolve APIException", e)
            }
        }
    }

    private fun onInitialized(s: StationService) {
        if ( !s.stationRepository.dataInitialized) {
            Toast.makeText(applicationContext, "Fail to init data", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // TODO init ui
        val fragment = LogFragment()
        supportFragmentManager.beginTransaction()
            .replace(R.id.layout_root, fragment)
            .commit()
        s.start()
    }

    override fun onResume() {
        super.onResume()
        viewModel.startService(this, this)
        viewModel.checkService(service.value, this, this::onInitialized)
    }

    override fun onDestroy() {
        super.onDestroy()
        service.value?.let {
            it.stop()
            service.set(null)
            unbindService(this)
        }
    }

    override fun finish() {
        service.value?.let {
            it.stop()
            service.set(null)
            unbindService(this)
        }
        stopService(Intent(this, StationService::class.java))
        super.finish()
    }

    private val viewModel: MainViewModel by lazy {
        getViewModelFactory(::MainViewModel).create(MainViewModel::class.java)
    }

    val service = ServiceGetter()

    override fun onServiceConnected(p0: ComponentName?, binder: IBinder?) {
        binder?.let {
            if (it is StationService.StationServiceBinder) {
                service.set( it.bind(this))
                service.value?.message("service connected with Activity")
                viewModel.checkService(service.value, this, this::onInitialized)
            }
        }
    }

    override fun onServiceDisconnected(p0: ComponentName?) {
        Log.d("Activity", "service disconnected")
    }

    companion object {
        const val PERMISSION_REQUEST_OVERLAY = 3900
        const val PERMISSION_REQUEST = 3901
        const val RESOLVE_API_EXCEPTION = 3902
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
        } else if ( requestCode == RESOLVE_API_EXCEPTION ){
            Log.d("ActivityResult", "resolve_api_exception")
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

    override fun onDialogButtonClicked(tag: String?, info: DataLatestInfo, which: Int) {
        tag?.let { viewModel.handleDialogButton(it, info, which, this) }
        viewModel.checkService(service.value, this, this::onInitialized)
    }

}

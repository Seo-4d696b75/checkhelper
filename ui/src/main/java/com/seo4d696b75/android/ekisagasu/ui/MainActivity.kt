package com.seo4d696b75.android.ekisagasu.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import com.seo4d696b75.android.ekisagasu.ui.log.LogViewModel
import com.seo4d696b75.android.ekisagasu.ui.navigation.MainScreen
import com.seo4d696b75.android.ekisagasu.ui.selectLine.LineSelectType
import com.seo4d696b75.android.ekisagasu.ui.service.StationService
import com.seo4d696b75.android.ekisagasu.ui.theme.AppTheme
import com.seo4d696b75.android.ekisagasu.ui.top.line.LineSelectDialogDirections
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * @author Seo-4d696b75
 * @version 2020/12/16.
 */
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private val viewModel: MainViewModel by viewModels()
    private val logViewModel: LogViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        // TODO activityの再生成が失敗するので暫定的に初期状態から
        super.onCreate(null)
        // setContentView(R.layout.main_activity)
        enableEdgeToEdge()

        setContent {
            AppTheme {
                MainScreen {
                    startService()
                    viewModel.checkData()
                }
            }
        }

        // listen to log file output request
        logViewModel
            .outputFileRequested
            .flowWithLifecycle(lifecycle)
            .onEach {
                requestLogFileUriLauncher.launch(it)
            }
            .launchIn(lifecycleScope)

        lifecycleScope.launch {
            viewModel
                .appFinish
                .flowWithLifecycle(lifecycle, Lifecycle.State.CREATED)
                .collect {
                    finish()
                }
        }
    }

    override fun onResume() {
        super.onResume()

        // handle intent
        intent?.let {
            if (it.getBooleanExtra(INTENT_KEY_SELECT_NAVIGATION, false)) {
                it.putExtra(INTENT_KEY_SELECT_NAVIGATION, false)
                val action = LineSelectDialogDirections.showLineSelectDialog(LineSelectType.Navigator)
                // findNavController(R.id.main_nav_host).navigate(action)
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

    private val requestLogFileUriLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        val uri = result.data?.data
        if (result.resultCode == Activity.RESULT_OK && uri != null) {
            Timber.d("log file resolved: $uri")
            logViewModel.onOutputFileResolved(uri, contentResolver)
        } else {
            Timber.w("Failed to resolved log file")
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_setting -> {
                findNavController(R.id.main_nav_host).navigate(R.id.goto_setting_screen)
            }

            R.id.menu_log -> {
                findNavController(R.id.main_nav_host).navigate(R.id.goto_log_screen)
            }

            else -> {
                Timber.tag("Menu").w("unknown id:${item.itemId}")
            }
        }
        return super.onOptionsItemSelected(item)
    }
}

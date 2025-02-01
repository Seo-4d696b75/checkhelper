package com.seo4d696b75.android.ekisagasu.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.seo4d696b75.android.ekisagasu.ui.navigation.MainScreen
import com.seo4d696b75.android.ekisagasu.ui.service.StationService
import com.seo4d696b75.android.ekisagasu.ui.theme.AppTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * @author Seo-4d696b75
 * @version 2020/12/16.
 */
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        // TODO activityの再生成が失敗するので暫定的に初期状態から
        super.onCreate(null)
        enableEdgeToEdge()

        setContent {
            AppTheme {
                MainScreen {
                    startService()
                    viewModel.checkData()
                }
            }
        }

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
                // TODO 路線選択ダイアログの表示
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
}

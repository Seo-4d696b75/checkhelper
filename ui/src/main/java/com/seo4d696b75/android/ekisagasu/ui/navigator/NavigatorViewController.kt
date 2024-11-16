package com.seo4d696b75.android.ekisagasu.ui.navigator

import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.seo4d696b75.android.ekisagasu.ui.MainActivity
import com.seo4d696b75.android.ekisagasu.ui.service.ServiceViewModelComponent
import com.seo4d696b75.android.ekisagasu.ui.service.viewModels
import com.seo4d696b75.android.ekisagasu.ui.theme.AppTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.ceil

class NavigatorViewController @Inject constructor(
    component: ServiceViewModelComponent,
) : ServiceViewModelComponent by component {
    private lateinit var windowManager: WindowManager
    private lateinit var view: View

    fun onCreate(
        context: Context,
        registryOwner: SavedStateRegistryOwner,
        lifecycleOwner: LifecycleOwner,
    ) {
        val layerType = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

        val layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            0,
            0,
            layerType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT,
        )
        layoutParams.gravity = Gravity.TOP or Gravity.START
        layoutParams.screenBrightness = -1f

        val viewModel: NavigatorViewModel by viewModels()

        val onSelectLineClicked = {
            val intent = Intent(context, MainActivity::class.java).apply {
                putExtra(MainActivity.INTENT_KEY_SELECT_NAVIGATION, true)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            context.startActivity(intent)
        }

        view = ComposeView(context).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setViewTreeSavedStateRegistryOwner(registryOwner)
            setViewTreeLifecycleOwner(lifecycleOwner)
            setContent {
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                AppTheme {
                    NavigatorScreen(
                        uiState = uiState,
                        onToggle = viewModel::onToggle,
                        onStopClicked = viewModel::onStopNavigator,
                        onSelectLineClicked = onSelectLineClicked,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }

        windowManager.addView(view, layoutParams)

        val density = context.resources.displayMetrics.density

        lifecycleOwner.lifecycleScope.launch {
            lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // 縮小時に背後へタップイベントを伝達するためViewのサイズを変更する必要がある
                // しかしwrap_contentではViewとComposeのサイズが連動しないため、Viewのサイズを直接指定する
                launch {
                    viewModel.isExpanded.drop(1).collectLatest {
                        val params = view.layoutParams as? WindowManager.LayoutParams ?: return@collectLatest
                        if (!it) {
                            delay(500)
                            params.width = ceil(59 * density).toInt()
                            params.height = ceil(59 * density).toInt()
                        } else {
                            params.width = WindowManager.LayoutParams.MATCH_PARENT
                            params.height = ceil(90 * density).toInt()
                        }
                        windowManager.updateViewLayout(view, params)
                    }
                }
                launch {
                    viewModel.isVisible.drop(1).collectLatest {
                        val params = view.layoutParams as? WindowManager.LayoutParams ?: return@collectLatest
                        if (!it) {
                            delay(500)
                            params.height = 1
                        } else {
                            params.height = ceil(90 * density).toInt()
                        }
                        windowManager.updateViewLayout(view, params)
                    }
                }
            }
        }
    }

    fun onDestroy() {
        windowManager.removeView(view)
    }
}

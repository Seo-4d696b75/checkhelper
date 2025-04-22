package com.seo4d696b75.android.ekisagasu.ui.popup

import android.content.Context
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.compose.runtime.getValue
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
import com.seo4d696b75.android.ekisagasu.ui.service.ServiceViewModelComponent
import com.seo4d696b75.android.ekisagasu.ui.service.viewModels
import com.seo4d696b75.android.ekisagasu.ui.theme.AppTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.ceil

@Singleton
class PopupViewController @Inject constructor(
    component: ServiceViewModelComponent,
) : ServiceViewModelComponent by component {
    private lateinit var windowManager: WindowManager
    private lateinit var view: View

    private val viewModel: PopupViewModel by viewModels()

    fun onCreate(
        context: Context,
        registryOwner: SavedStateRegistryOwner,
        lifecycleOwner: LifecycleOwner,
    ) {
        windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

        val layoutParam = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            0,
            0,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP.or(Gravity.START)
            screenBrightness = -1f
        }

        view = ComposeView(context).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setViewTreeSavedStateRegistryOwner(registryOwner)
            setViewTreeLifecycleOwner(lifecycleOwner)
            setContent {
                val uiState by viewModel.uiStateFlow.collectAsStateWithLifecycle()
                AppTheme {
                    PopupScreen(
                        uiState = uiState,
                        onClick = viewModel::onClicked,
                    )
                }
            }
        }

        windowManager.addView(view, layoutParam)

        val density = context.resources.displayMetrics.density

        lifecycleOwner.lifecycleScope.launch {
            lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // 縮小時に背後へタップイベントを伝達するためViewのサイズを変更する必要がある
                // しかしwrap_contentではViewとComposeのサイズが連動しないため、Viewのサイズを直接指定する
                launch {
                    viewModel.isExpanded.drop(1).collect {
                        layoutParam.width = if (it) {
                            WindowManager.LayoutParams.MATCH_PARENT
                        } else {
                            WindowManager.LayoutParams.WRAP_CONTENT
                        }
                        windowManager.updateViewLayout(view, layoutParam)
                    }
                }
                launch {
                    viewModel.isVisible.drop(1).collectLatest {
                        if (!it) {
                            delay(500)
                            layoutParam.height = 1
                        } else {
                            layoutParam.height = ceil(59 * density).toInt()
                        }
                        windowManager.updateViewLayout(view, layoutParam)
                    }
                }
            }
        }
    }

    fun onDestroy() {
        windowManager.removeView(view)
    }
}

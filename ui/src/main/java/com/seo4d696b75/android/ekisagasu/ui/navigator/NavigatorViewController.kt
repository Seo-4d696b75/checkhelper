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
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.seo4d696b75.android.ekisagasu.domain.navigator.NavigatorRepository
import com.seo4d696b75.android.ekisagasu.domain.navigator.NavigatorState
import com.seo4d696b75.android.ekisagasu.domain.search.StationSearchRepository
import com.seo4d696b75.android.ekisagasu.ui.MainActivity
import com.seo4d696b75.android.ekisagasu.ui.theme.AppTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

class NavigatorViewController @Inject constructor(
    private val searchRepository: StationSearchRepository,
    private val navigator: NavigatorRepository,
    private val getDisplayedNavigatorState: GetDisplayedNavigatorStateUseCase,
) {
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

        view = onCreateView(context, registryOwner, lifecycleOwner)
        windowManager.addView(view, layoutParams)
    }

    private fun onCreateView(
        context: Context,
        registryOwner: SavedStateRegistryOwner,
        lifecycleOwner: LifecycleOwner,
    ): View {
        val isVisible = MutableStateFlow(false)
        val isExpanded = MutableStateFlow(true)

        lifecycleOwner.lifecycleScope.launch {
            navigator
                .state
                .flowWithLifecycle(lifecycleOwner.lifecycle)
                .onEach { Timber.d("navigator state ${it.javaClass}") }
                .map { it is NavigatorState.Running }
                .distinctUntilChanged()
                .collect { running ->
                    if (running) {
                        isVisible.update { true }
                        isExpanded.update { true }
                    } else {
                        isVisible.update { false }
                    }
                }
        }

        val uiStateFlow = combine(
            isVisible,
            isExpanded,
            getDisplayedNavigatorState(),
        ) { visible, expanded, navigation ->
            NavigatorUiState(
                visible = visible,
                isExpanded = expanded,
                navigation = navigation,
            )
        }.stateIn(
            lifecycleOwner.lifecycleScope,
            SharingStarted.WhileSubscribed(),
            NavigatorUiState.Initial,
        )

        val onToggle = { isExpanded.update { !it } }
        val onStopClicked = {
            searchRepository.clearLine()
            navigator.stop()
        }
        val onSelectLineClicked = {
            val intent = Intent(context, MainActivity::class.java).apply {
                putExtra(MainActivity.INTENT_KEY_SELECT_NAVIGATION, true)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            context.startActivity(intent)
        }

        return ComposeView(context).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setViewTreeSavedStateRegistryOwner(registryOwner)
            setViewTreeLifecycleOwner(lifecycleOwner)
            setContent {
                val uiState by uiStateFlow.collectAsStateWithLifecycle()
                AppTheme {
                    NavigatorScreen(
                        uiState = uiState,
                        onToggle = onToggle,
                        onStopClicked = onStopClicked,
                        onSelectLineClicked = onSelectLineClicked,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }

    fun onDestroy() {
        windowManager.removeView(view)
    }
}

package com.seo4d696b75.android.ekisagasu.ui.navigator

import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.seo4d696b75.android.ekisagasu.domain.navigator.NavigatorRepository
import com.seo4d696b75.android.ekisagasu.domain.search.StationSearchRepository
import com.seo4d696b75.android.ekisagasu.ui.MainActivity
import com.seo4d696b75.android.ekisagasu.ui.R
import com.seo4d696b75.android.ekisagasu.ui.databinding.OverlayNavigatorBinding
import com.seo4d696b75.android.ekisagasu.ui.utils.setAnimationListener
import kotlinx.coroutines.launch
import javax.inject.Inject

class NavigatorViewController @Inject constructor(
    private val searchRepository: StationSearchRepository,
    private val navigator: NavigatorRepository,
    private val getNavigatorUiState: GetNavigatorUiStateUseCase,
) {
    private lateinit var windowManager: WindowManager
    private lateinit var icon: View
    private lateinit var binding: OverlayNavigatorBinding
    private lateinit var adapter: NavigatorStationAdapter

    private lateinit var animShrink: Animation
    private lateinit var animExpand: Animation
    private lateinit var animAppear: Animation
    private lateinit var animDisappear: Animation

    private var runningAnimation = false
    private var show = false

    fun onCreate(
        context: Context,
        owner: LifecycleOwner,
    ) {
        val inflater = LayoutInflater.from(context)
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
        binding = OverlayNavigatorBinding.inflate(inflater)
        binding.root.visibility = View.GONE
        windowManager.addView(binding.root, layoutParams)

        val iconLayoutParam = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            0,
            0,
            layerType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP.or(Gravity.START)
            screenBrightness = -1.0f
        }
        icon = inflater.inflate(R.layout.overlay_icon, null, false)
        icon.visibility = View.GONE
        windowManager.addView(icon, iconLayoutParam)

        binding.navigatorToggle.setOnClickListener {
            toggle()
        }
        icon.setOnClickListener {
            toggle()
        }
        binding.buttonNavigatorStop.setOnClickListener {
            searchRepository.selectLine(null)
            navigator.setLine(null)
        }
        binding.buttonNavigatorSelectLine.setOnClickListener {
            val intent = Intent(context, MainActivity::class.java).apply {
                putExtra(MainActivity.INTENT_KEY_SELECT_NAVIGATION, true)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            context.startActivity(intent)
        }

        animExpand = AnimationUtils.loadAnimation(context, R.anim.anim_expand)
        animShrink = AnimationUtils.loadAnimation(context, R.anim.anim_shrink)
        animAppear = AnimationUtils.loadAnimation(context, R.anim.anim_appear)
        animDisappear = AnimationUtils.loadAnimation(context, R.anim.anim_disappear)

        adapter = NavigatorStationAdapter(context).apply {
            setHasStableIds(true)
        }
        binding.listNavigatorPredication.also {
            it.layoutManager = LinearLayoutManager(context).apply {
                orientation = LinearLayoutManager.VERTICAL
                reverseLayout = true
            }
            it.adapter = adapter
        }

        owner.lifecycleScope.launch {
            launch {
                navigator
                    .isRunning
                    .flowWithLifecycle(owner.lifecycle)
                    .collect {
                        if (it) {
                            start()
                        } else {
                            stop()
                        }
                    }
            }
            launch {
                getNavigatorUiState()
                    .flowWithLifecycle(owner.lifecycle)
                    .collect {
                        update(it)
                    }
            }
        }
    }

    private fun start() {
        if (show) return
        show = true
        binding.root.visibility = View.VISIBLE
        icon.visibility = View.GONE
        animAppear.setAnimationListener(onEnd = {
            runningAnimation = false
        })
        binding.contentContainer.startAnimation(animAppear)
        runningAnimation = true
    }

    private fun update(state: NavigatorUiState) {
        if (!show) return
        binding.state = state
        val list = when (state) {
            is NavigatorUiState.Result -> state.stations
            else -> emptyList()
        }
        adapter.submitList(list)
    }

    private fun stop() {
        if (!show) return
        show = false
        if (binding.root.visibility == View.VISIBLE) {
            animDisappear.setAnimationListener(onEnd = {
                runningAnimation = false
                binding.root.visibility = View.GONE
            })
            binding.contentContainer.startAnimation(animDisappear)
            runningAnimation = true
        }
        icon.visibility = View.GONE
    }

    fun onDestroy() {
        windowManager.removeView(binding.root)
        windowManager.removeView(icon)
        binding.navigatorToggle.setOnClickListener(null)
        icon.setOnClickListener(null)
        binding.buttonNavigatorStop.setOnClickListener(null)
        binding.buttonNavigatorSelectLine.setOnClickListener(null)
        binding.listNavigatorPredication.adapter = null
    }

    private fun toggle() {
        if (runningAnimation) return
        if (binding.root.visibility == View.VISIBLE) {
            animShrink.setAnimationListener(onEnd = {
                binding.root.visibility = View.GONE
                runningAnimation = false
            })
            binding.contentContainer.startAnimation(animShrink)
            icon.visibility = View.VISIBLE
            runningAnimation = true
        } else if (binding.root.visibility == View.GONE) {
            animExpand.setAnimationListener(onEnd = {
                runningAnimation = false
            })
            binding.root.visibility = View.VISIBLE
            icon.visibility = View.GONE
            binding.contentContainer.startAnimation(animExpand)
            runningAnimation = true
        }
    }
}

package jp.seo.station.ekisagasu.ui.overlay

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.view.animation.AccelerateInterpolator
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.view.animation.DecelerateInterpolator
import android.widget.Button
import android.widget.TextView
import androidx.core.animation.addListener
import jp.seo.station.ekisagasu.Line
import jp.seo.station.ekisagasu.R
import jp.seo.station.ekisagasu.Station
import jp.seo.station.ekisagasu.position.PredictionResult
import jp.seo.station.ekisagasu.search.formatDistance
import jp.seo.station.ekisagasu.ui.MainActivity
import jp.seo.station.ekisagasu.utils.UnitLiveEvent
import jp.seo.station.ekisagasu.utils.setAnimationListener


/**
 * @author Seo-4d696b75
 * @version 2021/03/07.
 */
class NavigationView(
    ctx: Context,
    layerType: Int,
    private val windowManager: WindowManager,
    private val icon: View,
) {

    private val view: View
    private val line: TextView
    private val distances: Array<TextView>
    private val stations: Array<TextView>
    private val markers: Array<View>

    private val contentContainer: View
    private val stationContainer: View
    private val waitContainer: View

    private val animShrink: Animation
    private val animExpand: Animation
    private val animAppear: Animation
    private val animDisappear: Animation

    init {
        val inflater = LayoutInflater.from(ctx)
        val layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            0, 0, layerType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        )
        layoutParams.gravity = Gravity.TOP or Gravity.START
        layoutParams.screenBrightness = -1f
        view = inflater.inflate(R.layout.overlay_navigation, null, false)
        view.visibility = View.GONE
        windowManager.addView(view, layoutParams)

        contentContainer = view.findViewById(R.id.layout_navigation_content)
        stationContainer = view.findViewById(R.id.container_navigation_stations)
        waitContainer = view.findViewById(R.id.container_navigation_wait)
        line = view.findViewById(R.id.text_navigation_line)
        distances = arrayOf(
            view.findViewById(R.id.text_distance1),
            view.findViewById(R.id.text_distance2),
            view.findViewById(R.id.text_distance_fade)
        )
        stations = arrayOf(
            view.findViewById(R.id.text_station_current),
            view.findViewById(R.id.text_station_next1),
            view.findViewById(R.id.text_station_next2),
            view.findViewById(R.id.text_station_fade)
        )
        markers = arrayOf(
            view.findViewById<View>(R.id.station_marker_current).also {
                it.pivotX = 0f
                it.pivotY = 0f
            },
            view.findViewById<View>(R.id.station_marker_next1).also {
                it.pivotX = 0f
                it.pivotY = 0f
            },
            view.findViewById<View>(R.id.station_marker_next2).also {
                it.pivotX = 0f
                it.pivotY = 0f
            },
            view.findViewById<View>(R.id.station_marker_fade).also {
                it.pivotX = 0f
                it.pivotY = 0f
            }
        )

        view.setOnClickListener {
            toggleNavigation()
        }
        view.findViewById<Button>(R.id.button_navigation_select_line).setOnClickListener {
            val intent = Intent(ctx, MainActivity::class.java).apply {
                putExtra(MainActivity.INTENT_KEY_SELECT_NAVIGATION, true)
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            ctx.startActivity(intent)
        }
        view.findViewById<Button>(R.id.button_navigation_stop).setOnClickListener {
            stopNavigation.call()
        }


        animExpand = AnimationUtils.loadAnimation(ctx, R.anim.anim_expand)
        animShrink = AnimationUtils.loadAnimation(ctx, R.anim.anim_shrink)
        animAppear = AnimationUtils.loadAnimation(ctx, R.anim.anim_appear)
        animDisappear = AnimationUtils.loadAnimation(ctx, R.anim.anim_disappear)

    }

    val stopNavigation = UnitLiveEvent()
    private var currentStation: Station? = null
    private var runningAnimation = false
    private var runningAnimator: Animator? = null
    private var _show = false
    val show: Boolean = _show

    fun release() {
        windowManager.removeView(view)
        view.setOnClickListener(null)
    }

    fun toggleNavigation() {
        if (runningAnimation) return
        if (view.visibility == View.VISIBLE) {
            animShrink.setAnimationListener(onEnd = {
                view.visibility = View.GONE
                runningAnimation = false
            })
            contentContainer.startAnimation(animShrink)
            icon.visibility = View.VISIBLE
            runningAnimation = true
        } else if (view.visibility == View.GONE) {
            animExpand.setAnimationListener(onEnd = {
                icon.visibility = View.GONE
                runningAnimation = false
            })
            view.visibility = View.VISIBLE
            contentContainer.startAnimation(animExpand)
            runningAnimation = true
        }
    }

    fun startNavigation(line: Line, current: Station) {
        if (runningAnimation || _show) return
        this.line.text = line.name
        distances.forEach { it.text = "" }
        stations.forEach { it.text = "" }
        stations[0].text = current.name

        waitContainer.visibility = View.VISIBLE
        stationContainer.visibility = View.VISIBLE
        stations.forEach { it.text = "" }
        distances.forEach { it.text = "" }
        stations[0].text = current.name

        view.visibility = View.VISIBLE

        animAppear.setAnimationListener(onEnd = {
            runningAnimation = false
        })
        contentContainer.startAnimation(animAppear)
        _show = true
        runningAnimation = true
    }

    fun stopNavigation() {
        if (runningAnimation || !_show) return
        animDisappear.setAnimationListener(onEnd = {
            runningAnimation = false
            view.visibility = View.GONE
        })
        contentContainer.startAnimation(animDisappear)
        _show = false
        runningAnimation = true
        currentStation = null
    }

    fun onUpdate(result: PredictionResult) {
        if (!_show) return
        if (waitContainer.visibility == View.VISIBLE && result.size > 0) {
            waitContainer.visibility = View.GONE
        } else if (waitContainer.visibility != View.VISIBLE && result.size == 0) {
            waitContainer.visibility = View.VISIBLE
        }
        if (currentStation == null || currentStation == result.current || result.size == 0) {
            // no animation
            invalidate(result)
        } else {
            animate(result)
        }
        currentStation = result.current

    }

    private fun invalidate(result: PredictionResult) {
        stations[0].text = result.current.name
        if (result.size > 0) {
            stations[1].text = result.getStation(0).name
            distances[0].text = formatDistance(result.getDistance(0).toDouble())
        } else {
            stations[1].text = ""
            distances[0].text = ""
        }
        if (result.size > 1) {
            stations[2].text = result.getStation(1).name
            distances[1].text = formatDistance(result.getDistance(1).toDouble())
        } else {
            stations[2].text = ""
            distances[1].text = ""
        }
    }

    private fun animate(result: PredictionResult) {
        runningAnimator?.cancel()
        runningAnimator = null
        val distanceX = floatArrayOf(
            distances[0].x,
            distances[1].x,
            distances[2].x
        )
        val distanceY = floatArrayOf(
            distances[0].y,
            distances[1].y,
            distances[2].y
        )
        val nameX = floatArrayOf(
            stations[0].x,
            stations[1].x,
            stations[2].x,
            stations[3].x
        )
        val nameY = floatArrayOf(
            stations[0].y,
            stations[1].y,
            stations[2].y,
            stations[3].y
        )
        val markerX = floatArrayOf(
            markers[0].x,
            markers[1].x,
            markers[2].x,
            markers[3].x
        )
        val markerY = floatArrayOf(
            markers[0].y,
            markers[1].y,
            markers[2].y,
            markers[3].y
        )
        val scale = floatArrayOf(
            1f,
            markers[0].width.toFloat() / markers[1].width,
            markers[1].width.toFloat() / markers[2].width,
            markers[2].width.toFloat() / markers[3].width
        )
        // setting pivots for scaling
        distances[0].also {
            it.pivotX = 0f
            it.pivotY = it.height.toFloat()
        }
        distances[2].also {
            it.pivotX = it.width.toFloat()
            it.pivotY = 0f
        }
        stations[0].also {
            it.pivotX = 0f
            it.pivotY = it.height.toFloat()
        }
        stations[3].also {
            it.pivotX = it.width.toFloat()
            it.pivotY = 0f
        }
        val animatorSet = AnimatorSet()
        if (result.size > 1) {
            distances[2].text = formatDistance(result.getDistance(1).toDouble())
            stations[3].text = result.getStation(1).name
        }
        animatorSet.playTogether(
            ObjectAnimator.ofPropertyValuesHolder(
                distances[0],
                PropertyValuesHolder.ofFloat("scaleY", 1f, 0.5f),
                PropertyValuesHolder.ofFloat("scaleX", 1f, 0.5f),
                PropertyValuesHolder.ofFloat("alpha", 1f, 0.2f)
            ).also {
                it.interpolator = DecelerateInterpolator()
            },
            ObjectAnimator.ofPropertyValuesHolder(
                distances[1],
                PropertyValuesHolder.ofFloat("translationX", 0f, distanceX[0] - distanceX[1]),
                PropertyValuesHolder.ofFloat("translationY", 0f, distanceY[0] - distanceY[1])
            ),
            ObjectAnimator.ofPropertyValuesHolder(
                distances[2],
                PropertyValuesHolder.ofFloat("scaleY", 0.5f, 1f),
                PropertyValuesHolder.ofFloat("scaleX", 0.5f, 1f),
                PropertyValuesHolder.ofFloat("alpha", 0.2f, 1f)
            ).also {
                it.interpolator = AccelerateInterpolator()
            },
            ObjectAnimator.ofPropertyValuesHolder(
                stations[0],
                PropertyValuesHolder.ofFloat("scaleY", 1f, 0.5f),
                PropertyValuesHolder.ofFloat("scaleX", 1f, 0.5f),
                PropertyValuesHolder.ofFloat("alpha", 1f, 0.2f)
            ).also {
                it.interpolator = DecelerateInterpolator()
            },
            ObjectAnimator.ofPropertyValuesHolder(
                stations[1],
                PropertyValuesHolder.ofFloat("translationX", 0f, nameX[0] - nameX[1]),
                PropertyValuesHolder.ofFloat("translationY", 0f, nameY[0] - nameY[1])
            ),
            ObjectAnimator.ofPropertyValuesHolder(
                stations[2],
                PropertyValuesHolder.ofFloat("translationX", 0f, nameX[1] - nameX[2]),
                PropertyValuesHolder.ofFloat("translationY", 0f, nameY[1] - nameY[2])
            ),
            ObjectAnimator.ofPropertyValuesHolder(
                stations[3],
                PropertyValuesHolder.ofFloat("scaleY", 0.5f, 1f),
                PropertyValuesHolder.ofFloat("scaleX", 0.5f, 1f),
                PropertyValuesHolder.ofFloat("alpha", 0.2f, 1f)
            ),
            ObjectAnimator.ofPropertyValuesHolder(
                markers[3],
                PropertyValuesHolder.ofFloat("translationX", 0f, markerX[2] - markerX[3]),
                PropertyValuesHolder.ofFloat("translationY", 0f, markerY[2] - markerY[3]),
                PropertyValuesHolder.ofFloat("scaleX", 0.1f, scale[3]),
                PropertyValuesHolder.ofFloat("scaleY", 0.1f, scale[3]),
                PropertyValuesHolder.ofFloat("alpha", 0f, 1f)
            ),
            ObjectAnimator.ofPropertyValuesHolder(
                markers[2],
                PropertyValuesHolder.ofFloat("translationX", 0f, markerX[1] - markerX[2]),
                PropertyValuesHolder.ofFloat("translationY", 0f, markerY[1] - markerY[2]),
                PropertyValuesHolder.ofFloat("scaleX", 1f, scale[2]),
                PropertyValuesHolder.ofFloat("scaleY", 1f, scale[2])
            ),
            ObjectAnimator.ofPropertyValuesHolder(
                markers[1],
                PropertyValuesHolder.ofFloat("translationX", 0f, markerX[0] - markerX[1]),
                PropertyValuesHolder.ofFloat("translationY", 0f, markerY[0] - markerY[1]),
                PropertyValuesHolder.ofFloat("scaleX", 1f, scale[1]),
                PropertyValuesHolder.ofFloat("scaleY", 1f, scale[1])
            ),
            ObjectAnimator.ofPropertyValuesHolder(
                markers[0],
                PropertyValuesHolder.ofFloat("translationX", 0f, markerX[0] - markerX[1]),
                PropertyValuesHolder.ofFloat("translationY", 0f, markerY[0] - markerY[1])
            )
        )
        animatorSet.duration = 500L
        animatorSet.addListener(onStart = {
            distances[2].visibility = View.VISIBLE
            markers[3].visibility = View.VISIBLE
            stations[3].visibility = View.VISIBLE
        }, onEnd = {
            runningAnimator = null
            it.removeAllListeners()

            for (i in 0..2) {
                val view = distances[i]
                view.y = distanceY[i]
                view.x = distanceX[i]
                view.alpha = 1f
                view.scaleX = 1f
                view.scaleY = 1f
            }
            for (i in 0..3) {
                val view = stations[i]
                view.y = nameY[i]
                view.x = nameX[i]
                view.alpha = 1f
                view.scaleX = 1f
                view.scaleY = 1f
            }
            for (i in 0..3) {
                val view = markers[i]
                view.y = markerY[i]
                view.x = markerX[i]
                view.alpha = 1f
                view.scaleX = 1f
                view.scaleY = 1f
            }
            distances[2].visibility = View.INVISIBLE
            stations[3].visibility = View.INVISIBLE
            markers[3].visibility = View.INVISIBLE
            invalidate(result)
        })
        runningAnimator = animatorSet
        animatorSet.start()

    }
}

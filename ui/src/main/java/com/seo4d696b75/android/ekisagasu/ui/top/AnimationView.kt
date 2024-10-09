package com.seo4d696b75.android.ekisagasu.ui.top

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import android.os.Parcel
import android.os.Parcelable
import android.os.SystemClock
import android.util.AttributeSet
import android.view.View
import android.view.ViewTreeObserver
import com.seo4d696b75.android.ekisagasu.ui.R
import kotlin.math.ceil

/**
 * @author Seo-4d696b75
 * @version 2021/01/10.
 */
class AnimationView :
    View,
    ViewTreeObserver.OnGlobalLayoutListener {
    constructor(context: Context) : this(context, null)

    constructor(context: Context, set: AttributeSet?) : this(context, set, 0)

    constructor(context: Context, set: AttributeSet?, defaultAttr: Int) : super(
        context,
        set,
        defaultAttr,
    ) {

        viewTreeObserver.addOnGlobalLayoutListener(this)

        val res = context.resources
        foreground = BitmapFactory.decodeResource(res, R.drawable.launch_icon)
        background = BitmapFactory.decodeResource(res, R.drawable.loop_line)
        paint = Paint()
        _matrix = Matrix()

        setWillNotDraw(false)
    }

    private var _width: Int = 0
    private var _height: Int = 0
    private var _time: Long = 0
    private var _degree: Float = 0f

    private var running: Boolean = false
    private var requestRun: Boolean = false
    private var requestStop: Boolean = false

    private val background: Bitmap
    private val foreground: Bitmap
    private val _matrix: Matrix
    private val paint: Paint
    private var src: Rect? = null
    private var dst: Rect? = null

    override fun onGlobalLayout() {
        _width = super.getWidth()
        _height = super.getHeight()
        src = Rect(0, 0, foreground.width, foreground.height)
        dst =
            Rect(
                (_width * 0.1f).toInt(),
                (_height * 0.13f).toInt(),
                (_width * 0.8).toInt(),
                (_height * 0.83f).toInt(),
            )
        if (requestRun) runAnimation(true)
    }

    fun runAnimation(run: Boolean) {
        if (running != run) {
            if (running) {
                requestStop = true
            } else {
                if (_width == 0 || _height == 0) {
                    requestRun = true
                    return
                }
                running = true
                requestRun = false
                invalidate()
            }
        }
    }

    override fun onDraw(canvas: Canvas) {
        val dst = this.dst
        val src = this.src
        if (dst == null || src == null) return
        val time = SystemClock.uptimeMillis()
        if (running && _time > 0) {
            var degree = _degree + (time - _time) * 0.01f
            _time = time
            if (requestStop) {
                val step = 360f / 16f
                val th = ceil(_degree / step) * step
                if (degree > th) {
                    running = false
                    requestStop = false
                    _degree = 0f
                    _time = 0L
                }
            }
            if (degree > 360f) {
                degree -= 360f
            }
            _degree = degree
        } else {
            _time = time
        }
        _matrix.reset()
        _matrix.postRotate(_degree, background.width / 2f, background.height / 2f)
        _matrix.postScale(
            _width.toFloat() / background.width,
            _height.toFloat() / background.height,
        )
        canvas.drawBitmap(background, _matrix, paint)
        canvas.drawBitmap(foreground, src, dst, paint)

        if (running) invalidate()
    }

    override fun onRestoreInstanceState(source: Parcelable) {
        val state = source as SavedState
        super.onRestoreInstanceState(state.superState)
        runAnimation(state.mRunning)
    }

    override fun onSaveInstanceState(): Parcelable {
        val superState = super.onSaveInstanceState()
        val state = SavedState(superState)
        state.mRunning = this.running
        return state
    }

    private class SavedState : BaseSavedState {
        var mRunning = false

        constructor(state: Parcelable?) : super(state)
        constructor(source: Parcel) : super(source) {
            val array = BooleanArray(1)
            source.readBooleanArray(array)
            mRunning = array[0]
        }

        override fun writeToParcel(
            out: Parcel,
            flags: Int,
        ) {
            super.writeToParcel(out, flags)
            val array = booleanArrayOf(mRunning)
            out.writeBooleanArray(array)
        }

        companion object {
            @JvmField
            val CREATOR: Parcelable.Creator<SavedState?> =
                object : Parcelable.Creator<SavedState?> {
                    override fun createFromParcel(`in`: Parcel): SavedState = SavedState(`in`)

                    override fun newArray(size: Int): Array<SavedState?> = arrayOfNulls(size)
                }
        }
    }
}

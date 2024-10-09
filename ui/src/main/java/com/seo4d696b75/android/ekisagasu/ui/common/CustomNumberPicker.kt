package com.seo4d696b75.android.ekisagasu.ui.common

import android.annotation.SuppressLint
import android.content.Context
import android.os.Parcel
import android.os.Parcelable
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.NumberPicker
import androidx.annotation.CallSuper
import com.seo4d696b75.android.ekisagasu.ui.R
import java.lang.Integer.max
import java.lang.Integer.min
import kotlin.math.abs
import kotlin.math.pow

/**
 * 負数まで拡張した整数全体を表示・選択できる[android.widget.NumberPicker]の拡張View
 *
 * @author Seo-4d696b75
 * @version 2022/10/24
 */
open class CustomNumberPicker : NumberPicker {

    constructor(context: Context) : super(context) {
        init(context, null, 0)
    }

    constructor(context: Context, set: AttributeSet?) : super(context, set) {
        init(context, set, 0)
    }

    constructor(context: Context, set: AttributeSet?, defaultAttr: Int) : super(
        context,
        set,
        defaultAttr
    ) {
        init(context, set, defaultAttr)
    }

    private fun init(context: Context, set: AttributeSet?, defaultAttr: Int) {
        val array =
            context.obtainStyledAttributes(set, R.styleable.CustomNumberPicker, defaultAttr, 0)
        min = array.getInteger(R.styleable.CustomNumberPicker_min, 0)
        max = array.getInteger(R.styleable.CustomNumberPicker_max, 100)
        step = array.getInteger(R.styleable.CustomNumberPicker_step, 10)
        speed = array.getFloat(R.styleable.CustomNumberPicker_scrollSpeed, 1f)
        shiftPoint = array.getInteger(R.styleable.CustomNumberPicker_speedShiftPoint, 1)
        shiftRate = array.getFloat(R.styleable.CustomNumberPicker_speedShiftRate, 1f)
        val value = array.getInteger(R.styleable.CustomNumberPicker_value, 0)
        array.recycle()
        setValues(false)
        setValue(value)
    }

    private var min: Int = 0
    private var max: Int = 100
    private var step: Int = 1
    private var valueSet: Array<Int> = arrayOf()
    private var speed: Float = 1.0f
    private var lastYPos: Float = 1.0f
    private var shiftPoint: Int = 1
    private var shiftRate: Float = 1.0f
    private var currentSpeed: Float = 0.0f

    private var listener: OnValueChangeListener? = null

    override fun setOnValueChangedListener(listener: OnValueChangeListener?) {
        if (listener == null) {
            this.listener = null
            super.setOnValueChangedListener(null)
        } else {
            this.listener = listener
            super.setOnValueChangedListener { _, old, new ->
                this.listener?.onValueChange(this, valueSet[old], valueSet[new])
            }
        }
    }

    private fun setValues(keepValue: Boolean) {
        var value = 0
        if (keepValue) {
            value = this.displayedValue
        }
        // 最大最小の逆転を修正
        if (min > max) {
            val temp: Int = min
            min = max
            max = temp
        }
        // ステップ
        if (step <= 0 || step > max - min) {
            step = 1
        }
        // 最大最小をステップに合わせる、つまり上限下限と同じ扱いにする
        if (max > 0) {
            max = max / step * step
        } else if (max < 0) {
            max = if (-max % step == 0) max else -(-max / step + 1) * step
        }
        if (min > 0) {
            min = if (min % step == 0) min else (min / step + 1) * step
        } else if (min < 0) {
            min = -(-min / step) * step
        }
        // 空集合の場合は初期値に変更
        if (min > max) {
            min = 0
            max = if (step > 100) step else 100
        }
        val length: Int = (max - min) / step + 1
        valueSet = Array(length) {
            min + step * it
        }
        super.setDisplayedValues(null)
        // super#setDisplayedValues した配列の長さより大きな範囲になる場合、IndexOutOfBounds で死ぬので一度nullにしておく
        super.setMinValue(0)
        super.setMaxValue(length - 1)
        super.setDisplayedValues(getDisplayedValues(length))
        if (keepValue) {
            setValue(value)
        }
    }

    open fun getDisplayedValues(length: Int): Array<String> {
        return Array(length) {
            (min + step * it).toString()
        }
    }

    private fun getClosestIndex(request: Int): Int {
        val value = max(min(this.max, request), this.min)
        val v = if (value > 0) {
            if (value % step >= (step + 1) / 2) (value / step + 1) * step else value / step * step
        } else if (value < 0) {
            if (-value % step <= step / 2) -(-value / step) * step else -(-value / step + 1) * step
        } else {
            0
        }
        return (v - min) / step
    }

    @Deprecated(
        """returned value is not the displayed value,
            but an index of current displayed value, use 'displayedValue' instead.""",
        ReplaceWith("displayedValue")
    )
    override fun getValue(): Int {
        // implementation of super class depends on this method,
        // DO NOT override this!
        return super.getValue()
    }

    override fun setValue(value: Int) {
        // in super class, #setValueInternal used
        displayedValue = value
    }

    /**
     * Viewが表示・選択している値
     *
     * ### `setter`
     * [ステップ][stepValue]の倍数で最も近い値に調整される
     */
    var displayedValue: Int
        get() {
            val i = super.getValue()
            return valueSet[i]
        }
        set(value) {
            val i = getClosestIndex(value)
            super.setValue(i)
        }

    override fun setMinValue(minValue: Int) {
        displayedMinValue = minValue
    }

    @Deprecated(
        "returned value is always 0, use 'displayedMinValue' instead",
        ReplaceWith("displayedMinValue")
    )
    override fun getMinValue(): Int {
        // implementation of super class depends on this method,
        // DO NOT override this!
        return super.getMinValue()
    }

    /**
     * Viewが表示・選択できる最小の値
     *
     * [NumberPicker]と異なり負数も許容する
     *
     * ### `setter`
     * [ステップ][stepValue]の倍数で最も近い値に調整される
     */
    var displayedMinValue: Int
        get() = min
        set(value) {
            min = value
            setValues(true)
        }

    override fun setMaxValue(maxValue: Int) {
        displayedMaxValue = maxValue
    }

    @Deprecated(
        "returned value is max index of displayed values, use 'displayedMaxValue' instead",
        ReplaceWith("displayedMaxValue")
    )
    override fun getMaxValue(): Int {
        // implementation of super class depends on this method,
        // DO NOT override this!
        return super.getMaxValue()
    }

    /**
     * Viewが表示・選択できる最大の値
     *
     * ### `setter`
     * [ステップ][stepValue]の倍数で最も近い値に調整される
     */
    var displayedMaxValue: Int
        get() = max
        set(value) {
            max = value
            setValues(true)
        }

    /**
     * Viewが表示・選択する値のステップ
     *
     * この値の倍数のみ表示・選択できるように制限する.
     * 1以上の整数値
     */
    var stepValue: Int
        get() = step
        set(value) {
            step = value
            setValues(true)
        }

    /**
     * スクロール（wheel scroll）の速度を調整する
     *
     * `0`だと操作できないため、`abs(value) > 0.1`の条件を課す.
     * 負数の場合はスクロール方向が逆になる
     */
    var scrollSpeed: Float
        get() = speed
        set(value) {
            if (abs(value) >= 0.1f) {
                speed = value
            }
        }

    var scrollSpeedShiftSteps: Int
        get() = shiftPoint
        set(shift) {
            if (shift > 0) {
                shiftPoint = shift
            }
        }

    var scrollSpeedShiftRate: Float
        get() = shiftRate
        set(rate) {
            if (rate > 1f) {
                shiftRate = rate
            }
        }

    @SuppressLint("ClickableViewAccessibility")
    @CallSuper
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        when (event?.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                lastYPos = event.y
                val x = event.x
                val width = super.getWidth()
                val section = shiftPoint - 1 - (x * shiftPoint / width).toInt()
                val index = shiftRate.pow(section)
                currentSpeed = speed * index
            }

            MotionEvent.ACTION_MOVE -> {
                val currentMoveY = event.y
                val deltaMoveY = ((currentMoveY - lastYPos) * (currentSpeed - 1f)).toInt()
                super.scrollBy(0, deltaMoveY)
                lastYPos = currentMoveY
            }

            else -> {
            }
        }
        return super.onTouchEvent(event)
    }

    class SavedState : BaseSavedState {
        constructor(superState: Parcelable?) : super(superState)
        private constructor(source: Parcel) : super(source) {
            min = source.readInt()
            max = source.readInt()
            step = source.readInt()
            speed = source.readFloat()
            shiftPoint = source.readInt()
            shiftRate = source.readFloat()
            value = source.readInt()
        }

        var min = 0
        var max = 0
        var step = 0
        var speed = 0f
        var shiftPoint = 0
        var shiftRate = 0f
        var value = 0

        override fun writeToParcel(out: Parcel, flags: Int) {
            super.writeToParcel(out, flags)
            out.writeInt(min)
            out.writeInt(max)
            out.writeInt(step)
            out.writeFloat(speed)
            out.writeInt(shiftPoint)
            out.writeFloat(shiftRate)
            out.writeInt(value)
        }

        override fun toString(): String {
            val id = Integer.toHexString(System.identityHashCode(this))
            return "CustomNumberPicker\$SavedState@$id(value=$value, min=$min, max=$max, step=$step, speed=$speed)"
        }

        companion object {
            @JvmField
            val CREATOR: Parcelable.Creator<SavedState?> =
                object : Parcelable.Creator<SavedState?> {
                    override fun createFromParcel(source: Parcel): SavedState {
                        return SavedState(source)
                    }

                    override fun newArray(size: Int): Array<SavedState?> {
                        return arrayOfNulls(size)
                    }
                }
        }
    }

    override fun onSaveInstanceState(): Parcelable? {
        val superState = super.onSaveInstanceState()
        val state = SavedState(superState)
        state.min = displayedMinValue
        state.max = displayedMaxValue
        state.value = displayedValue
        state.step = stepValue
        state.speed = scrollSpeed
        state.shiftPoint = shiftPoint
        state.shiftRate = shiftRate
        return state
    }

    override fun onRestoreInstanceState(state: Parcelable) {
        val myState = state as SavedState
        super.onRestoreInstanceState(myState.superState)
        min = myState.min
        max = myState.max
        step = myState.step
        speed = myState.speed
        shiftPoint = myState.shiftPoint
        shiftRate = myState.shiftRate
        setValues(false)
        value = myState.value
        requestLayout()
    }
}

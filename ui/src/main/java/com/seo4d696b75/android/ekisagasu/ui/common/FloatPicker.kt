package com.seo4d696b75.android.ekisagasu.ui.common

import android.content.Context
import android.os.Parcel
import android.os.Parcelable
import android.util.AttributeSet
import com.seo4d696b75.android.ekisagasu.ui.R
import kotlin.math.absoluteValue
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.round

/**
 * @author Seo-4d696b75
 * @version 2022/10/24.
 */
class FloatPicker : CustomNumberPicker {

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

        val array = context.obtainStyledAttributes(set, R.styleable.FloatPicker, defaultAttr, 0)
        max = array.getFloat(R.styleable.FloatPicker_maxFloat, 10f)
        min = array.getFloat(R.styleable.FloatPicker_minFloat, 0f)
        val step = array.getFloat(R.styleable.FloatPicker_stepFloat, 0.1f)
        value = array.getFloat(R.styleable.FloatPicker_valueFloat, 0f)
        array.recycle()
        super.stepValue = 1
        stepFloat = step
    }


    private var step = 0f
    private var min = 0f
    private var max = 0f
    private var value = 0f
    private var initialized = false
    private var digits = 0

    private var _listener: OnFloatValueChangedListener? = null

    fun setOnValueChangedListener(listener: OnFloatValueChangedListener?) {
        if (listener == null) {
            _listener = null
            super.setOnValueChangedListener(null)
        } else {
            _listener = listener
            super.setOnValueChangedListener { _, old, new ->
                _listener?.invoke(this, old * step, new * step)
            }
        }
    }

    @Deprecated(
        "this listener does now work, use 'OnFloatValueChangedListener' instead.",
        ReplaceWith("setOnValueChangedListener { picker, oldValue, newValue -> }")
    )
    override fun setOnValueChangedListener(listener: OnValueChangeListener?) {
    }

    override fun getDisplayedValues(length: Int): Array<String> {
        return if (initialized) {
            Array(length) {
                (min + step * it).format
            }
        } else {
            super.getDisplayedValues(length)
        }
    }

    // 小数点以下の桁数を調整して文字列表現に変換
    private val Float.format: String
        get() = String.format("%.${digits}f", value)

    /**
     * Viewが表示・選択できる数値のステップ（粒度）
     *
     * ## `setter`
     *
     * 正数のみ. ここで指定した値に従って表示する小数点以下桁数を調整する.
     * ステップの値に合わせて[最小値][minFloat],[最大値][maxFloat]が変化する場合がある
     *
     */
    var stepFloat: Float
        get() = step
        set(step) {
            if (step < 0f) return
            this.step = step
            digits = -log10(step).toInt()
            var value = step * 10.0f.pow(digits)
            while ((value - round(value)).absoluteValue > 0.00001f) {
                digits++
                value *= 10f
            }
            initialized = true
            minFloat = min
            maxFloat = max
            valueFloat = this.value
        }

    /**
     * Viewが表示・選択できる数値の最小値
     *
     * ## `setter`
     * 最も近い[ステップ][stepFloat]の倍数に調整する
     */
    var minFloat: Float
        get() = min
        set(min) {
            super.setMinValue(round(min / step).toInt())
            this.min = super.displayedMinValue * step
        }

    /**
     * Viewが表示・選択できる数値の最大値
     *
     * ## `setter`
     * 最も近い[ステップ][stepFloat]の倍数に調整する
     *
     */
    var maxFloat: Float
        get() = max
        set(max) {
            super.setMaxValue(round(max / step).toInt())
            this.max = super.displayedMaxValue * step
        }

    /**
     * Viewが表示・選択している値
     *
     * ## `setter`
     * [[最小値][minFloat],[最大値][maxFloat]]の区間で最も近い[ステップ][stepFloat]の倍数に調整する
     *
     */
    var valueFloat: Float
        get() {
            this.value = super.displayedValue * step
            return this.value
        }
        set(value) {
            super.setValue(round(value / step).toInt())
            this.value = super.displayedValue * step
        }

    @Deprecated(
        "use 'valueFloat' property instead",
        ReplaceWith("valueFloat = 1.0f")
    )
    override fun setValue(value: Int) {
    }

    @Deprecated("use 'valueFloat' property instead", ReplaceWith("valueFloat"))
    override fun getValue(): Int {
        @Suppress("deprecation")
        return super.getValue()
    }

    @Deprecated(
        "use 'maxFloat' property instead",
        ReplaceWith("maxFloat = 1.0f")
    )
    override fun setMaxValue(maxValue: Int) {
    }

    @Deprecated(
        "use 'maxFloat' property instead",
        ReplaceWith("maxFloat")
    )
    override fun getMaxValue(): Int {
        @Suppress("deprecation")
        return super.getMaxValue()
    }

    @Deprecated(
        "use 'minFloat' property instead",
        ReplaceWith("minFloat = 1.0f")
    )
    override fun setMinValue(minValue: Int) {
    }

    @Deprecated(
        "use 'minFloat' property instead",
        ReplaceWith("minFloat")
    )
    override fun getMinValue(): Int {
        @Suppress("deprecation")
        return super.getMinValue()
    }


    class SavedState : BaseSavedState {
        constructor(superState: Parcelable?) : super(
            superState
        )

        private constructor(source: Parcel) : super(
            source,
            // このクラスローダーはsuperState(CustomNumberPicker$SavedState)のforClassで使う
            // 明示的に指定しないとClassNotFoundExceptionで落ちる場合あり
            CustomNumberPicker::class.java.classLoader,
        ) {
            min = source.readFloat()
            max = source.readFloat()
            value = source.readFloat()
            step = source.readFloat()
        }

        var min = 0f
        var max = 0f
        var value = 0f
        var step = 0f

        override fun writeToParcel(out: Parcel, flags: Int) {
            super.writeToParcel(out, flags)
            out.writeFloat(min)
            out.writeFloat(max)
            out.writeFloat(value)
            out.writeFloat(step)
        }

        override fun toString(): String {
            val id = Integer.toHexString(System.identityHashCode(this))
            return "FloatPicker\$SavedState@$id(value=$value, min=$min, max=$max, step=$step)"
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

    override fun onSaveInstanceState(): Parcelable {
        //setFreezesText(true);
        val superState = super.onSaveInstanceState()
        val state = SavedState(superState)
        state.value = valueFloat
        state.step = stepFloat
        state.min = minFloat
        state.max = maxFloat
        return state
    }

    override fun onRestoreInstanceState(state: Parcelable) {
        val myState = state as SavedState
        super.onRestoreInstanceState(myState.superState)
        value = myState.value
        min = myState.min
        max = myState.max
        super.stepValue = 1
        stepFloat = myState.step
        requestLayout()
    }
}

typealias OnFloatValueChangedListener =
            (picker: FloatPicker, oldValue: Float, newValue: Float) -> Unit

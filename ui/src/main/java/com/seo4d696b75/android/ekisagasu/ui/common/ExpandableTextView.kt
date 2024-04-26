package com.seo4d696b75.android.ekisagasu.ui.common

import android.content.Context
import android.os.Parcel
import android.os.Parcelable
import android.text.TextPaint
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.text.buildSpannedString
import com.seo4d696b75.android.ekisagasu.ui.R
import timber.log.Timber
import java.lang.Float.max
import java.lang.Float.min
import java.util.Locale
import kotlin.math.ceil


/**
 * 文字列の長さに応じてViewの横幅や文字列の横方向の比率を動的に制御できるTextViewの拡張.
 *
 * **NOTE** [setText(char[], int, int)][setText]は非対応なので他のオーバロードを呼ぶ
 * ## `layout_width`による挙動の違い
 *
 * - `wrap_content`
 *   文字列の長さが許容される最大幅を超える場合は文字列を伸縮させるが、超えないなら文字列長さにView幅を合わせる
 * - それ以外
 *   このLayoutParamから計算される値で横幅を決定し、この長さを超える文字列は横方向に縮小させて収める
 *
 * ## XMLで定義できる属性一覧
 *
 * - maxWidth
 *   `layout_width=wrap_content`時のみ有効でこの幅を超える文字列が[setText]されるとこの幅に収まるように`textScaleX`を制御する
 * - minTextScaleX
 *   この比率を下回らないよう指定する.デフォルト値は0.5 この比率でも文字列がView幅を超える場合は超過部分を'…'で置換する
 *
 *
 * @author Seo-4d696b75
 * @version 2019/04/18.
 */
class ExpandableTextView : AppCompatTextView {

    constructor(context: Context) : this(context, null) {

    }

    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0) {

    }

    constructor(context: Context, attrs: AttributeSet?, defaultAttr: Int) : super(
        context,
        attrs,
        0
    ) {

        val array = context.obtainStyledAttributes(
            attrs,
            R.styleable.ExpandableTextView,
            defaultAttr,
            0
        )
        _maxWidth = array.getDimensionPixelSize(
            R.styleable.ExpandableTextView_maxWidth,
            Int.MAX_VALUE
        )
        _minTextScaleX = array.getFloat(R.styleable.ExpandableTextView_minTextScaleX, 0.5f)
        array.recycle()

        _minTextScaleX = max(_minTextScaleX, 0.1f)
        _minTextScaleX = min(_minTextScaleX, 1f)

        textPaint = super.getPaint()
        super.setLines(1)
        _currentText = ""
        _displayedText = ""

        super.setTextScaleX(textScaleX / 2f)
    }

    private var _maxWidth: Int
    private var _minTextScaleX: Float

    private val textPaint: TextPaint

    private var requestMeasure: Boolean = false
    private var rawWidthMeasureSpec: Int = 0
    private var calcWidthMeasureSpec: Int = 0

    private var _currentText: CharSequence
    private var _displayedText: CharSequence


    /**
     * このViewが表示するよう指定されている文字列を取得する
     * @return [setText]で指定した値
     */
    override fun getText(): CharSequence {
        return _currentText
    }

    /**
     * Viewが表示している文字列
     *
     * Viewの横幅に制限があり、かつ文字列を横方向に収縮しても収まらない場合は
     * [setText]で指定した値とは別の代替文字列を表示しているときがある
     */
    val displayedText: CharSequence
        get() = _displayedText

    override fun setMaxWidth(maxPixels: Int) {
        if (maxPixels <= 0) return
        if (maxPixels != _maxWidth) {
            _maxWidth = maxPixels
            requestMeasure = true
            super.setMaxWidth(maxPixels)
        }
    }

    /**
     * 文字列の`textScaleX`の最小値
     *
     * `[0.1, 1.0]`の値に制限される
     */
    var minTextScaleX: Float
        get() = _minTextScaleX
        set(scale) {
            if (scale < 0.1f || scale > 1f) return
            if (_minTextScaleX != scale) {
                if (MeasureSpec.getMode(rawWidthMeasureSpec) == MeasureSpec.EXACTLY) {
                    // no layout() needed
                    updateText(
                        _currentText, BufferType.NORMAL, MeasureSpec.getSize(
                            rawWidthMeasureSpec
                        )
                    )
                } else {
                    // has to measure again
                    requestMeasure = true
                    requestLayout()
                }
            }
        }

    override fun setText(text: CharSequence?, type: BufferType?) {
        //#setText(char[], int, int) <- finalでオーバーライド不可
        //           以外はここを経由している
        val value = text ?: ""
        if (_currentText == value) return
        _currentText = value

        if (MeasureSpec.getMode(rawWidthMeasureSpec) == MeasureSpec.EXACTLY) {
            // no layout() needed
            updateText(value, type ?: BufferType.NORMAL, MeasureSpec.getSize(rawWidthMeasureSpec))
        } else {
            requestMeasure = true
            super.setText(value, type)
        }
    }


    // 必要に応じて文字列を伸縮させたり代替文字列に置換する
    private fun updateText(original: CharSequence, type: BufferType, widthSize: Int) {
        var text = original
        textPaint.textScaleX = 1f
        val length: Float = textPaint.measureText(text, 0, text.length)
        val padding = compoundPaddingLeft + compoundPaddingRight
        if (length + padding > widthSize) {
            val scale = (widthSize - padding) / length
            text = modifyText(scale, text, (widthSize - padding).toFloat())
        }
        _displayedText = text
        super.setText(text, type)
    }


    /**
     * 指定された最大幅に合うように文字列を横方向へ収縮させる.
     * 収縮率が[.mMinTextScaleX]を下回る場合は、その最小比率でちょうど最大幅に合うような代替文字列に置換する
     * @param scale     rough value, with which text width can be adjusted to maxLength
     * @param text      raw text
     * @param maxLength max length of text
     * @return modified text
     */
    private fun modifyText(scale: Float, text: CharSequence, maxLength: Float): CharSequence {
        if (scale <= 0 || maxLength <= 0) return ""
        return if (calcTextScaleX(scale, text, maxLength)) {
            // 適切なスケールで全体が収まるならOK
            text
        } else {
            // ダメなら短縮
            return reduceString(text, maxLength)
        }
    }

    /**
     * 指定された最大幅に合うように文字列の横方向への収縮を試みる.<br></br>
     * [android.widget.TextView.setTextScaleX]で指定するTextScaleXと
     * [TextPaint.measureText]で測定される文字列の横幅は比例しない？
     * 適当に当たりをつけて倍率を探索して、許容範囲内で最善の値を採択する
     *
     * @return true if [[.mMinTextScaleX],1]の範囲で適切な倍率が存在する
     */
    private fun calcTextScaleX(rough: Float, text: CharSequence, maxLength: Float): Boolean {
        if (rough < _minTextScaleX || rough > 1f) return false
        textPaint.textScaleX = rough
        var length: Float = textPaint.measureText(text, 0, text.length)
        val step = 0.01f
        if (length < maxLength) {
            var cnt = 1
            while (true) {
                val scale = rough * (1f + cnt * step)
                if (scale > 1f) {
                    textPaint.textScaleX = 1f
                    return true
                }
                textPaint.textScaleX = scale
                length = textPaint.measureText(text, 0, text.length)
                if (length > maxLength) break
                cnt++
            }
            textPaint.textScaleX = (rough * (1f + (cnt - 1) * step))
            return true
        } else if (length == maxLength) {
            return true
        } else {
            var cnt = 1
            while (true) {
                val scale = rough * (1f - cnt * step)
                if (scale < _minTextScaleX) return false
                textPaint.textScaleX = scaleX
                length = textPaint.measureText(text, 0, text.length)
                if (length <= maxLength) return true
                cnt++
            }
        }
    }

    /**
     * 最低倍率で文字列を伸縮してもViewの横幅に収まらない場合は代替文字列を用意する.
     * 具体的には、文字列の末端から1文字以上を削除して'…'に置換する.
     * 当然文字によって横幅は異なるので、削るべき文字数は簡単には計算できない.
     * そこで、View横幅に収まるまで削る文字数を1文字ずつ増やしながら探索する
     * @return modified text, whose width can be adjusted with proper scaleX
     */
    private fun reduceString(text: CharSequence, maxLength: Float): CharSequence {
        val builder = StringBuilder()
        builder.append(text)
        var i = text.length
        while (i > 0) {
            builder.deleteCharAt(i - 1)
            builder.append('…')
            val str = builder.toString()
            textPaint.textScaleX = _minTextScaleX
            val length: Float = textPaint.measureText(str)
            if (length <= maxLength) {
                calcTextScaleX(_minTextScaleX, str, maxLength)
                // String型で長さを計算するが、SpannableStringなどの装飾要素を最後に調整する
                return buildSpannedString {
                    append(text.subSequence(0, i - 1))
                    append('…')
                }
            }
            builder.deleteCharAt(i - 1)
            i--
        }
        return ""
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        /* 幅に関してのみ要求を追加 */

        // measure text length if only text or requested spec has been changed
        if (requestMeasure || widthMeasureSpec != rawWidthMeasureSpec) {
            val widthMode = MeasureSpec.getMode(widthMeasureSpec)
            val widthSize = MeasureSpec.getSize(widthMeasureSpec)
            requestMeasure = false
            val text: CharSequence = _currentText
            textPaint.textScaleX = 1f
            val length: Float = textPaint.measureText(text, 0, text.length)
            val padding = compoundPaddingLeft + compoundPaddingRight
            var requestedWidth = 0
            when (widthMode) {
                MeasureSpec.EXACTLY -> {
                    // no need to care about its content
                    requestedWidth = widthSize
                    if (widthMeasureSpec != rawWidthMeasureSpec) {
                        // if view width changes, update its content
                        updateText(_currentText, BufferType.NORMAL, widthSize)
                    }
                }

                MeasureSpec.AT_MOST -> {
                    val max = kotlin.math.min(widthSize, _maxWidth)
                    if (length + padding > max) {
                        requestedWidth = max
                        val scale = (max - padding) / length
                        val modified = modifyText(scale, text, (max - padding).toFloat())
                        if (_displayedText != modified) {
                            _displayedText = modified
                            super.setText(modified, BufferType.NORMAL)
                        }
                    } else {
                        requestedWidth = ceil((length + padding).toDouble()).toInt()
                    }
                }

                MeasureSpec.UNSPECIFIED -> {
                    requestedWidth = ceil((length + padding).toDouble()).toInt()
                    textPaint.textScaleX = 1f
                    _displayedText = text
                }
            }
            rawWidthMeasureSpec = widthMeasureSpec
            calcWidthMeasureSpec = MeasureSpec.makeMeasureSpec(requestedWidth, MeasureSpec.EXACTLY)
        }

        /* super.onMeasure()の細かい実装は親に任せる */
        super.onMeasure(calcWidthMeasureSpec, heightMeasureSpec)
    }

    @Deprecated(
        "value fixed: lines = 1",
        ReplaceWith("// Do not call ExpandableTextView#setLines(Int)")
    )
    override fun setLines(lines: Int) {
        Timber.tag("ExpandableTextView").w("setLines(Int) has no effect on this widget")
    }

    override fun toString(): String {
        return String.format(
            Locale.US, "ExpandableTextView@%x{displayed:\"%s\",text:\"%s\",textScaleX=%.2f}",
            System.identityHashCode(this),
            _displayedText, _currentText, textPaint.textScaleX
        )
    }

    @Deprecated(
        "textScaleX is adjusted automatically",
        ReplaceWith("// Do not call ExpandableTextView#setTextScaleX(Float)")
    )
    override fun setTextScaleX(size: Float) {
        Timber.tag("ExpandableTextView").w("textScaleX is adjusted automatically, so DO NOT set manually")
    }


    internal class SavedState : BaseSavedState {
        constructor(superState: Parcelable?) : super(superState) {}
        private constructor(source: Parcel) : super(source) {
            mMinScale = source.readFloat()
            mMaxWidth = source.readInt()
            mText = source.readString()
        }

        var mMinScale = 0f
        var mMaxWidth = 0
        var mText: String? = null
        override fun writeToParcel(out: Parcel, flags: Int) {
            super.writeToParcel(out, flags)
            out.writeFloat(mMinScale)
            out.writeInt(mMaxWidth)
            out.writeString(mText)
        }

        override fun toString(): String {
            return String.format(
                Locale.US,
                "ExpandableTextView#SavedState{%s text:%s}",
                Integer.toHexString(System.identityHashCode(this)), mText
            )
        }

        companion object {
            @JvmField
            val CREATOR: Parcelable.Creator<SavedState?> = object : Parcelable.Creator<SavedState?> {
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
        val superState = super.onSaveInstanceState()
        val state = SavedState(superState)
        state.mText = _currentText.toString()
        state.mMaxWidth = _maxWidth
        state.mMinScale = _minTextScaleX
        return state
    }

    override fun onRestoreInstanceState(state: Parcelable) {
        val myState = state as SavedState
        super.onRestoreInstanceState(myState.superState)
        _minTextScaleX = myState.mMinScale
        _maxWidth = myState.mMaxWidth
        setText(myState.mText)
    }

}

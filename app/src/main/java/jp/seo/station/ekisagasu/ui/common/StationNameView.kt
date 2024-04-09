package jp.seo.station.ekisagasu.ui.common

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.util.TypedValue
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import com.seo4d696b75.android.ekisagasu.data.station.Station
import com.seo4d696b75.android.ekisagasu.ui.common.ExpandableTextView
import jp.seo.station.ekisagasu.R
import kotlin.math.max

/**
 * @author Seo-4d696b75
 * @version 2019/04/18.
 */
class StationNameView : LinearLayout {
    constructor(context: Context) : super(context) {
        init(context, null, 0)
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init(context, attrs, 0)
    }

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(
        context,
        attrs,
        defStyle,
    ) {
        init(context, attrs, defStyle)
    }

    private lateinit var nameText: TextView
    private lateinit var kanaText: TextView

    private fun init(
        context: Context,
        attrs: AttributeSet?,
        defStyle: Int,
    ) {
        // Load attributes
        val array =
            context.obtainStyledAttributes(
                attrs,
                R.styleable.StationNameView,
                defStyle,
                0,
            )
        val nameTextSize =
            array.getDimensionPixelSize(R.styleable.StationNameView_nameTextSize, 20).toFloat()
        val kanaTextSize =
            array.getDimensionPixelSize(R.styleable.StationNameView_kanaTextSize, 20).toFloat()
        val nameTextColor = array.getColor(R.styleable.StationNameView_nameTextColor, Color.BLACK)
        val kanaTextColor =
            array.getColor(R.styleable.StationNameView_kanaTextColor, Color.BLACK)
        array.recycle()

        orientation = VERTICAL

        val param =
            LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            )

        val name = ExpandableTextView(context)
        name.setTextSize(TypedValue.COMPLEX_UNIT_PX, nameTextSize)
        name.setTextColor(nameTextColor)
        addView(name, param)
        nameText = name

        val kana = ExpandableTextView(context)
        kana.setTextSize(TypedValue.COMPLEX_UNIT_PX, kanaTextSize)
        kana.setTextColor(kanaTextColor)
        addView(kana, param)
        kanaText = kana
    }

    fun setStation(station: Station) {
        nameText.text = station.name
        kanaText.text = station.nameKana
    }

    override fun onLayout(
        changed: Boolean,
        l: Int,
        t: Int,
        r: Int,
        b: Int,
    ) {
        val w1: Int = nameText.measuredWidth
        val w2: Int = kanaText.measuredWidth
        val h1: Int = nameText.measuredHeight
        val h2: Int = kanaText.measuredHeight
        val w = max(w1, w2)
        nameText.layout(
            (w - w1) / 2,
            0,
            (w - w1) / 2 + w1,
            h1,
        )
        kanaText.layout(
            (w - w2) / 2,
            h1,
            w2 + (w - w2) / 2,
            h1 + h2,
        )
    }
}

package jp.seo.station.ekisagasu.ui.common

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import jp.seo.station.ekisagasu.Line
import jp.seo.station.ekisagasu.R
import jp.seo.station.ekisagasu.utils.parseColorCode

class LineAdapter(
    context: Context,
    lines: List<Line>
) : ArrayAdapter<Line>(context, 0, lines) {

    private val inflater = LayoutInflater.from(context)

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: kotlin.run {
            inflater.inflate(R.layout.cell_line, parent, false)
        }
        getItem(position)?.let { line ->
            view.findViewById<TextView>(R.id.text_cell_line_name).text = line.name
            view.findViewById<TextView>(R.id.text_cell_line_stations_size).text =
                String.format("%d駅", line.stationSize)
            val symbol = view.findViewById<TextView>(R.id.text_cell_line_symbol)
            symbol.background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 6f
                setColor(parseColorCode(line.color))
            }
        }
        return view
    }
}
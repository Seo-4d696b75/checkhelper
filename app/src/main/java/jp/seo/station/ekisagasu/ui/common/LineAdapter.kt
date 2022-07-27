package jp.seo.station.ekisagasu.ui.common

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import jp.seo.station.ekisagasu.model.Line
import jp.seo.station.ekisagasu.R
import jp.seo.station.ekisagasu.databinding.CellLineSmallBinding
import jp.seo.station.ekisagasu.utils.parseColorCode

class LineComparator: DiffUtil.ItemCallback<Line>() {
    override fun areItemsTheSame(oldItem: Line, newItem: Line): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: Line, newItem: Line): Boolean {
        return oldItem == newItem
    }
}

class LineNameViewHolder(val binding: CellLineSmallBinding) : RecyclerView.ViewHolder(binding.root)

class LineNameAdapter(context: Context) : ListAdapter<Line, LineNameViewHolder>(LineComparator()){
    private val inflater = LayoutInflater.from(context)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LineNameViewHolder {
        val binding = DataBindingUtil.inflate<CellLineSmallBinding>(
            inflater,
            R.layout.cell_line_small,
            parent,
            false
        )
        return LineNameViewHolder(binding)
    }

    override fun onBindViewHolder(holder: LineNameViewHolder, position: Int) {
        val line = getItem(position)
        holder.binding.line = line
    }

    fun getLine(position: Int): Line = getItem(position)

}

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
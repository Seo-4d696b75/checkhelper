package jp.seo.station.ekisagasu.ui.common

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import jp.seo.station.ekisagasu.R
import jp.seo.station.ekisagasu.model.StationRegister

class StationAdapter(context: Context, stations: List<StationRegister>) :
    ArrayAdapter<StationRegister>(context, 0, stations) {

    private val inflater: LayoutInflater = LayoutInflater.from(context)

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: inflater.inflate(R.layout.cell_station, parent, false)
        getItem(position)?.let { r ->
            view.findViewById<TextView>(R.id.text_cell_station_name).text = r.station.name
            view.findViewById<TextView>(R.id.text_cell_station_numbering).text = r.numbering
        }
        return view
    }
}

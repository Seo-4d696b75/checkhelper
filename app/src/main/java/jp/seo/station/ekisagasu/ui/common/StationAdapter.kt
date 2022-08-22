package jp.seo.station.ekisagasu.ui.common

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.databinding.DataBindingUtil
import jp.seo.station.ekisagasu.R
import jp.seo.station.ekisagasu.databinding.CellStationBinding
import jp.seo.station.ekisagasu.model.StationRegister

class StationAdapter(context: Context, stations: List<StationRegister>) :
    ArrayAdapter<StationRegister>(context, 0, stations) {

    private val inflater: LayoutInflater = LayoutInflater.from(context)

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val binding = if (convertView == null) {
            DataBindingUtil.inflate<CellStationBinding>(
                inflater,
                R.layout.cell_station,
                parent,
                false,
            )
        } else {
            convertView.tag as CellStationBinding
        }
        getItem(position)?.let { r ->
            binding.numbering = r.numbering
            binding.stationName = r.station.name
        }
        return binding.root
    }
}

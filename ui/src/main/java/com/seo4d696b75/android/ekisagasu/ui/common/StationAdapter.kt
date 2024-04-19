package com.seo4d696b75.android.ekisagasu.ui.common

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import com.seo4d696b75.android.ekisagasu.ui.databinding.CellStationBinding

class StationAdapter(context: Context, stations: List<StationRegistrationUiState>) :
    ArrayAdapter<StationRegistrationUiState>(context, 0, stations) {
    private val inflater: LayoutInflater = LayoutInflater.from(context)

    override fun getView(
        position: Int,
        convertView: View?,
        parent: ViewGroup,
    ): View {
        val binding = if (convertView == null) {
            CellStationBinding.inflate(inflater)
        } else {
            convertView.tag as CellStationBinding
        }
        getItem(position)?.let { r ->
            binding.numbering = r.numbering
            binding.stationName = r.station.name
        }
        binding.root.tag = binding
        return binding.root
    }
}

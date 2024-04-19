package com.seo4d696b75.android.ekisagasu.ui.top.radar

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.seo4d696b75.android.ekisagasu.domain.search.NearStation
import com.seo4d696b75.android.ekisagasu.ui.databinding.CellStationRadarBinding

class RadarAdapter(context: Context) : ListAdapter<NearStation, RadarAdapter.RadarViewHolder>(
    NearStationComparator()
) {
    private val inflater = LayoutInflater.from(context)
    var onItemClickListener: ((NearStation) -> Unit)? = null

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): RadarViewHolder {
        val binding = CellStationRadarBinding.inflate(inflater)
        return RadarViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: RadarViewHolder,
        position: Int,
    ) {
        val near = getItem(position)
        holder.binding.index = (position + 1).toString()
        holder.binding.near = near
        holder.binding.root.setOnClickListener {
            onItemClickListener?.invoke(near)
        }
    }

    override fun getItemId(position: Int): Long = getItem(position).station.code.toLong()

    private class NearStationComparator : DiffUtil.ItemCallback<NearStation>() {
        override fun areItemsTheSame(
            oldItem: NearStation,
            newItem: NearStation,
        ): Boolean {
            return oldItem.station.id == newItem.station.id && oldItem.distance == newItem.distance
        }

        override fun areContentsTheSame(
            oldItem: NearStation,
            newItem: NearStation,
        ): Boolean {
            return oldItem == newItem
        }
    }

    class RadarViewHolder(val binding: CellStationRadarBinding) : RecyclerView.ViewHolder(binding.root)
}

package com.seo4d696b75.android.ekisagasu.ui.navigator

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.seo4d696b75.android.ekisagasu.ui.databinding.CellStationNavigatorBinding

class NavigatorStationAdapter(
    context: Context,
) : ListAdapter<NavigatorStationUiState, NavigatorStationAdapter.NavigatorStationViewHolder>(
    NavigatorStationComparator(),
) {

    init {
        setHasStableIds(true)
    }

    private val inflater = LayoutInflater.from(context)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NavigatorStationViewHolder {
        val binding = CellStationNavigatorBinding.inflate(inflater)
        return NavigatorStationViewHolder(binding)
    }

    override fun onBindViewHolder(holder: NavigatorStationViewHolder, position: Int) {
        val item = getItem(position)
        holder.binding.state = item
    }

    override fun getItemId(position: Int): Long {
        // 現在駅 or 予測駅の区別は無視して同じ駅なら同じリスト要素と見なす
        return getItem(position).station.code.toLong()
    }

    private class NavigatorStationComparator : DiffUtil.ItemCallback<NavigatorStationUiState>() {
        override fun areItemsTheSame(
            oldItem: NavigatorStationUiState,
            newItem: NavigatorStationUiState,
        ): Boolean {
            return when {
                oldItem is NavigatorStationUiState.Current &&
                    newItem is NavigatorStationUiState.Current -> {
                    oldItem.station.id == newItem.station.id
                }

                oldItem is NavigatorStationUiState.Prediction &&
                    newItem is NavigatorStationUiState.Prediction -> {
                    oldItem.station.id == newItem.station.id &&
                        oldItem.distance == newItem.distance
                }

                else -> false
            }
        }

        override fun areContentsTheSame(
            oldItem: NavigatorStationUiState,
            newItem: NavigatorStationUiState,
        ): Boolean {
            return oldItem == newItem
        }

    }

    class NavigatorStationViewHolder(
        val binding: CellStationNavigatorBinding,
    ) : RecyclerView.ViewHolder(binding.root)
}

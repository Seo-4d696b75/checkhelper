package com.seo4d696b75.android.ekisagasu.ui.log.history

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.seo4d696b75.android.ekisagasu.domain.log.AppLogTarget
import com.seo4d696b75.android.ekisagasu.ui.R
import com.seo4d696b75.android.ekisagasu.ui.databinding.CellListHistoryBinding

class HistoryAdapter(context: Context) :
    ListAdapter<AppLogTarget, HistoryAdapter.AppHistoryViewHolder>(AppLogTargetComparator()) {
    var onItemSelectedListener: ((AppLogTarget) -> Unit)? = null

    private val inflater = LayoutInflater.from(context)

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): AppHistoryViewHolder {
        val binding = DataBindingUtil.inflate<CellListHistoryBinding>(
            inflater,
            R.layout.cell_list_history,
            parent,
            false,
        )
        return AppHistoryViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: AppHistoryViewHolder,
        position: Int,
    ) {
        val log = getItem(position)
        holder.binding.data = log
        holder.binding.running = (position == 0)
        holder.binding.appHistoryItemContainer.setOnClickListener {
            onItemSelectedListener?.invoke(log)
        }
    }

    class AppHistoryViewHolder(val binding: CellListHistoryBinding) : RecyclerView.ViewHolder(binding.root)

    private class AppLogTargetComparator : DiffUtil.ItemCallback<AppLogTarget>() {
        override fun areItemsTheSame(
            oldItem: AppLogTarget,
            newItem: AppLogTarget,
        ): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(
            oldItem: AppLogTarget,
            newItem: AppLogTarget,
        ): Boolean {
            return oldItem == newItem
        }
    }
}

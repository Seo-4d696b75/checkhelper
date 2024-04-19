package com.seo4d696b75.android.ekisagasu.ui.log

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.seo4d696b75.android.ekisagasu.domain.log.AppLog
import com.seo4d696b75.android.ekisagasu.ui.databinding.CellListLogBinding

class LogAdapter(context: Context) : ListAdapter<AppLog, LogAdapter.LogViewHolder>(AppLogComparator()) {
    private val inflater = LayoutInflater.from(context)

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): LogViewHolder {
        val binding = CellListLogBinding.inflate(inflater)
        return LogViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: LogViewHolder,
        position: Int,
    ) {
        val log = getItem(position)
        holder.binding.data = log
    }

    override fun getItemId(position: Int): Long = getItem(position).id

    class LogViewHolder(val binding: CellListLogBinding) : RecyclerView.ViewHolder(binding.root)

    private class AppLogComparator : DiffUtil.ItemCallback<AppLog>() {
        override fun areItemsTheSame(
            oldItem: AppLog,
            newItem: AppLog,
        ): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(
            oldItem: AppLog,
            newItem: AppLog,
        ): Boolean {
            return oldItem == newItem
        }
    }
}

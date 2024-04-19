package com.seo4d696b75.android.ekisagasu.ui.common

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.seo4d696b75.android.ekisagasu.domain.dataset.Line
import com.seo4d696b75.android.ekisagasu.ui.databinding.CellLineBinding
import com.seo4d696b75.android.ekisagasu.ui.databinding.CellLineSmallBinding

class LineComparator : DiffUtil.ItemCallback<Line>() {
    override fun areItemsTheSame(
        oldItem: Line,
        newItem: Line,
    ): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(
        oldItem: Line,
        newItem: Line,
    ): Boolean {
        return oldItem == newItem
    }
}

class LineNameViewHolder(val binding: CellLineSmallBinding) : RecyclerView.ViewHolder(binding.root)

class LineNameAdapter(context: Context) : ListAdapter<Line, LineNameViewHolder>(LineComparator()) {
    private val inflater = LayoutInflater.from(context)

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): LineNameViewHolder {
        val binding = CellLineSmallBinding.inflate(inflater)
        return LineNameViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: LineNameViewHolder,
        position: Int,
    ) {
        val line = getItem(position)
        holder.binding.line = line
    }

    fun getLine(position: Int): Line = getItem(position)
}

class LineAdapter(context: Context, lines: List<Line>) : ArrayAdapter<Line>(context, 0, lines) {
    private val inflater = LayoutInflater.from(context)

    override fun getView(
        position: Int,
        convertView: View?,
        parent: ViewGroup,
    ): View {
        val binding = if (convertView == null) {
            CellLineBinding.inflate(inflater)
        } else {
            convertView.tag as CellLineBinding
        }
        binding.line = getItem(position)
        binding.root.tag = binding
        return binding.root
    }
}

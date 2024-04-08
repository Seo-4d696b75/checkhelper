package com.seo4d696b75.android.ekisagasu.ui.common

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView


/**
 * @author Seo-4d696b75
 * @version 2018/10/31.
 */
class HorizontalListView : RecyclerView {


    constructor(context: Context) : this(context, null) {
    }

    constructor(context: Context, set: AttributeSet?) : this(context, set, 0) {
    }

    constructor(context: Context, set: AttributeSet?, defaultAttr: Int) : super(
        context,
        set,
        defaultAttr
    ) {

        val manager = LinearLayoutManager(context)
        manager.orientation = LinearLayoutManager.HORIZONTAL
        layoutManager = manager

    }

    private var listener: OnItemClickListener? = null

    fun setOnItemClickListener(listener: OnItemClickListener?) {
        this.listener = listener

        val adapter = this.adapter
        if (adapter is ArrayAdapter<*>) {
            adapter.setOnItemSelectedListener { view: View, data: Any?, pos: Int ->
                this.listener?.let { it(view, pos) }
            }
        }
    }

    override fun setAdapter(adapter: Adapter<*>?) {
        super.setAdapter(adapter)
        this.listener?.let {
            setOnItemClickListener(it)
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        listener = null
        adapter = null
        layoutManager = null
    }

    private class SimpleViewHolder(
        view: View
    ) : ViewHolder(view) {}

    abstract class ArrayAdapter<E> : Adapter<ViewHolder> {

        constructor() : super() {
            list = listOf()
        }

        constructor(data: List<E>) : super() {
            list = data
        }

        private val views: MutableList<View> = ArrayList()
        private var list: List<E>
        var data: List<E>
            get() = list
            set(value) {
                list = value
                notifyDataSetChanged()
            }

        /**
         * リストの要素のViewをインスタンス化する
         */
        abstract fun getView(group: ViewGroup): View

        /**
         * リストに表示するデータをViewに反映する
         */
        abstract fun onBindView(view: View, data: E, position: Int)

        fun getItem(position: Int): E {
            return list[position]
        }

        private var listener: OnItemSelectedListener<E>? = null

        fun setOnItemSelectedListener(listener: OnItemSelectedListener<E>?) {
            this.listener = listener
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = getView(parent)
            views.add(view)
            return SimpleViewHolder(getView(parent))
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val view = holder.itemView
            val data = getItem(position)
            onBindView(view, data, position)

            view.setOnClickListener {
                listener?.let { it(view, data, holder.adapterPosition) }
            }
        }

        override fun getItemCount(): Int {
            return list.size
        }

        override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
            super.onDetachedFromRecyclerView(recyclerView)
            views.forEach { it.setOnClickListener(null) }
            views.clear()
            listener = null
        }

    }

}

typealias OnItemSelectedListener<E> = (view: View, data: E, position: Int) -> Unit

typealias OnItemClickListener = (view: View, position: Int) -> Unit

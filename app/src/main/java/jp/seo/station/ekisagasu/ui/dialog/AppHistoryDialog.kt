package jp.seo.station.ekisagasu.ui.dialog

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.seo4d696b75.android.ekisagasu.domain.log.AppLogTarget
import dagger.hilt.android.AndroidEntryPoint
import jp.seo.station.ekisagasu.R
import jp.seo.station.ekisagasu.databinding.CellListHistoryBinding
import jp.seo.station.ekisagasu.databinding.DialogHistoryBinding
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/**
 * @author Seo-4d696b75
 * @version 2021/02/04.
 */
@AndroidEntryPoint
class AppHistoryDialog : DialogFragment() {
    private val viewModel: AppHistoryViewModel by viewModels()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val context = requireContext()
        val binding =
            DataBindingUtil.inflate<DialogHistoryBinding>(
                layoutInflater,
                R.layout.dialog_history,
                null,
                false,
            )
        val adapter = HistoryAdapter(context).apply {
            onItemSelectedListener = {
                viewModel.setLogTarget(it)
                dismiss()
            }
        }
        binding.listRebootHistory.also {
            it.addItemDecoration(
                DividerItemDecoration(
                    context,
                    LinearLayoutManager.VERTICAL,
                ),
            )
            it.layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
            it.adapter = adapter
        }

        viewModel.history
            .flowWithLifecycle(lifecycle)
            .onEach {
                adapter.submitList(it)
            }
            .launchIn(lifecycleScope)

        return AlertDialog.Builder(context).apply {
            setView(binding.root)
            setTitle(R.string.dialog_title_history)
            setMessage(R.string.dialog_message_history)
            setNegativeButton(R.string.dialog_button_cancel) { _, _ ->
                dismiss()
            }
        }.create()
    }

    class AppHistoryViewHolder(val binding: CellListHistoryBinding) : RecyclerView.ViewHolder(binding.root)

    class AppLogTargetComparator : DiffUtil.ItemCallback<AppLogTarget>() {
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

    class HistoryAdapter(context: Context) :
        ListAdapter<AppLogTarget, AppHistoryViewHolder>(AppLogTargetComparator()) {
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
    }
}

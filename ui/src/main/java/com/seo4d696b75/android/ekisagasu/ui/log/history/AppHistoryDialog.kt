package com.seo4d696b75.android.ekisagasu.ui.log.history

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.seo4d696b75.android.ekisagasu.domain.log.AppLogTarget
import com.seo4d696b75.android.ekisagasu.ui.R
import com.seo4d696b75.android.ekisagasu.ui.databinding.CellListHistoryBinding
import com.seo4d696b75.android.ekisagasu.ui.databinding.DialogHistoryBinding
import dagger.hilt.android.AndroidEntryPoint
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
        val binding = DialogHistoryBinding.inflate(layoutInflater)
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
}

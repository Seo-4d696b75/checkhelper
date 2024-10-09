package com.seo4d696b75.android.ekisagasu.ui.log

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.seo4d696b75.android.ekisagasu.domain.log.AppLogType
import com.seo4d696b75.android.ekisagasu.ui.databinding.FragmentLogBinding
import com.seo4d696b75.android.ekisagasu.ui.log.history.AppHistoryDialogDirections
import com.seo4d696b75.android.ekisagasu.ui.log.output.LogOutputConfDialogDirections
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/**
 * @author Seo-4d696b75
 * @version 2020/12/21.
 */
@AndroidEntryPoint
class LogFragment : Fragment() {
    private val viewModel: LogViewModel by activityViewModels()

    private lateinit var binding: FragmentLogBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentLogBinding.inflate(inflater)
        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner
        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        val context = requireContext()

        binding.dropdownLogFilter.apply {
            val values = AppLogType.Filter.entries.map { it.name }
            val adapter = ArrayAdapter(context, android.R.layout.simple_spinner_dropdown_item, values)
            setAdapter(adapter)
            setOnItemClickListener { _, _, position, _ ->
                val name = adapter.getItem(position) ?: throw NoSuchElementException()
                val filter = AppLogType.Filter.valueOf(name)
                viewModel.setLogFilter(filter)
            }
            setSelection(0)
        }

        binding.listLog.also {
            it.addItemDecoration(
                DividerItemDecoration(
                    context,
                    LinearLayoutManager.VERTICAL,
                ),
            )
            val adapter =
                LogAdapter(context).apply {
                    setHasStableIds(true)
                }
            it.layoutManager =
                LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false).apply {
                    stackFromEnd = true
                }
            it.adapter = adapter
            viewModel.logs
                .flowWithLifecycle(viewLifecycleOwner.lifecycle)
                .onEach {
                    adapter.submitList(it)
                }
                .launchIn(viewLifecycleOwner.lifecycleScope)
        }

        binding.buttonWriteLog.setOnClickListener {
            viewModel.requestWriteLog {
                val action = LogOutputConfDialogDirections.showLogOutputConfigDialog(
                    LogOutputConfigArg(it)
                )
                findNavController().navigate(action)
            }
        }

        binding.textLogFilterSince.setOnClickListener {
            val action = AppHistoryDialogDirections.showAppHistoryDialog()
            findNavController().navigate(action)
        }
    }
}

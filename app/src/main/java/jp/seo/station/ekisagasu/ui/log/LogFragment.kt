package jp.seo.station.ekisagasu.ui.log

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.*
import dagger.hilt.android.AndroidEntryPoint
import jp.seo.station.ekisagasu.R
import jp.seo.station.ekisagasu.database.AppLog
import jp.seo.station.ekisagasu.databinding.CellListLogBinding
import jp.seo.station.ekisagasu.databinding.FragmentLogBinding
import jp.seo.station.ekisagasu.ui.dialog.AppHistoryDialogDirections
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/**
 * @author Seo-4d696b75
 * @version 2020/12/21.
 */
@ExperimentalCoroutinesApi
@AndroidEntryPoint
class LogFragment : Fragment() {

    private val viewModel: LogViewModel by viewModels()

    private lateinit var binding: FragmentLogBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(
            layoutInflater,
            R.layout.fragment_log,
            container,
            false,
        )
        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val context = requireContext()

        binding.dropdownLogFilter.apply {
            val values = LogFilter.all.map { it.name }
            val adapter =
                ArrayAdapter(context, android.R.layout.simple_spinner_dropdown_item, values)
            setAdapter(adapter)
            setOnItemClickListener { _, _, position, _ ->
                val name = adapter.getItem(position)!!
                val filter =
                    LogFilter.all.find { it.name == name } ?: throw NoSuchElementException()
                viewModel.setLogFilter(filter)
            }
            setSelection(0)
        }

        binding.listLog.also {
            it.addItemDecoration(
                DividerItemDecoration(
                    context,
                    LinearLayoutManager.VERTICAL,
                )
            )
            val adapter = LogAdapter(context).apply {
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
            viewModel.requestWriteLog(getString(R.string.app_name))
        }

        binding.textLogFilterSince.setOnClickListener {
            val action = AppHistoryDialogDirections.actionGlobalAppHistoryDialog()
            findNavController().navigate(action)
        }

        viewModel.onLogFileUriResolved
            .flowWithLifecycle(viewLifecycleOwner.lifecycle)
            .onEach {
                viewModel.writeLog(it, context.contentResolver)
            }
            .launchIn(viewLifecycleOwner.lifecycleScope)
    }

    class LogViewHolder(val binding: CellListLogBinding) : RecyclerView.ViewHolder(binding.root)

    class AppLogComparator : DiffUtil.ItemCallback<AppLog>() {
        override fun areItemsTheSame(oldItem: AppLog, newItem: AppLog): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: AppLog, newItem: AppLog): Boolean {
            return oldItem == newItem
        }
    }

    class LogAdapter(context: Context) :
        ListAdapter<AppLog, LogViewHolder>(AppLogComparator()) {

        private val inflater = LayoutInflater.from(context)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LogViewHolder {
            val binding = DataBindingUtil.inflate<CellListLogBinding>(
                inflater,
                R.layout.cell_list_log,
                parent,
                false,
            )
            return LogViewHolder(binding)
        }

        override fun onBindViewHolder(holder: LogViewHolder, position: Int) {
            val log = getItem(position)
            holder.binding.data = log
        }

        override fun getItemId(position: Int): Long {
            return getItem(position).id
        }
    }
}

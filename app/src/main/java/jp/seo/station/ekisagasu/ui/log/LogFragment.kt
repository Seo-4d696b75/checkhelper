package jp.seo.station.ekisagasu.ui.log

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.android.AndroidEntryPoint
import jp.seo.station.ekisagasu.R
import jp.seo.station.ekisagasu.database.AppLog
import jp.seo.station.ekisagasu.databinding.FragmentLogBinding
import jp.seo.station.ekisagasu.ui.dialog.AppHistoryDialogDirections
import jp.seo.station.ekisagasu.utils.TIME_PATTERN_MILLI_SEC
import jp.seo.station.ekisagasu.utils.formatTime
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
        binding.listLog.apply {
            addItemDecoration(
                DividerItemDecoration(
                    context,
                    LinearLayoutManager.VERTICAL,
                )
            )
        }

        binding.spinnerFilterLog.also {
            val adapter = LogTypeAdapter(context, LogFilter.all)
            it.adapter = adapter
            it.setOnItemClickListener { _, _, position, _ ->
                val filter = adapter.getItem(position)!!
                viewModel.setLogFilter(filter)
            }
        }

        binding.listLog.also {
            val adapter = LogAdapter(context, emptyList())
            it.layoutManager =
                LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false).apply {
                    stackFromEnd = true
                }
            it.adapter = adapter
            viewModel.logs
                .flowWithLifecycle(viewLifecycleOwner.lifecycle)
                .onEach {
                    adapter.logs = it
                    adapter.notifyDataSetChanged()
                }
                .launchIn(viewLifecycleOwner.lifecycleScope)
        }

        binding.buttonWriteLog.setOnClickListener {
            viewModel.requestWriteLog(getString(R.string.app_name))
        }

        binding.buttonSelectHistory.setOnClickListener {
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

}

class LogTypeAdapter(
    context: Context,
    values: Array<LogFilter>
) : ArrayAdapter<LogFilter>(context, 0, values) {

    private val inflater = LayoutInflater.from(context)

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val value = getItem(position)
        val view =
            convertView ?: inflater.inflate(android.R.layout.simple_spinner_item, null, false)
        if (value != null && view is TextView) {
            view.text = value.name
        }
        return view
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        val value = getItem(position)
        val view = convertView ?: inflater.inflate(
            android.R.layout.simple_spinner_dropdown_item,
            null,
            false
        )
        if (value != null && view is TextView) {
            view.text = value.name
        }
        return view
    }

}

class LogViewHolder(view: View) : RecyclerView.ViewHolder(view) {

    val time: TextView = view.findViewById(R.id.text_log_time)
    val message: TextView = view.findViewById(R.id.text_log_message)
}

class LogAdapter(context: Context, var logs: List<AppLog>) :
    RecyclerView.Adapter<LogViewHolder>() {

    private val inflater = LayoutInflater.from(context)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LogViewHolder {
        val view = inflater.inflate(R.layout.cell_list_log, parent, false)
        return LogViewHolder(view)
    }

    override fun onBindViewHolder(holder: LogViewHolder, position: Int) {
        val log = logs[position]
        holder.time.text = formatTime(TIME_PATTERN_MILLI_SEC, log.timestamp)
        holder.message.text = log.message
    }

    override fun getItemCount(): Int {
        return logs.size
    }

}

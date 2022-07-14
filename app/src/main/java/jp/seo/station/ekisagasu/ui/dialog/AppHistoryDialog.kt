package jp.seo.station.ekisagasu.ui.dialog

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import jp.seo.station.ekisagasu.R
import jp.seo.station.ekisagasu.database.AppRebootLog
import jp.seo.station.ekisagasu.databinding.DialogHistoryBinding
import jp.seo.station.ekisagasu.utils.TIME_PATTERN_DATETIME
import jp.seo.station.ekisagasu.utils.formatTime
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
        val binding = DataBindingUtil.inflate<DialogHistoryBinding>(
            layoutInflater,
            R.layout.dialog_history,
            null,
            false,
        )
        val builder = AlertDialog.Builder(context)
        builder.setView(binding.root)
        val adapter = HistoryAdapter(context, emptyList())
        binding.listRebootHistory.adapter = adapter
        binding.listRebootHistory.setOnItemClickListener { _, _, position, _ ->
            val item = adapter.getItem(position)!!
            viewModel.setLogTarget(item)
            dismiss()
        }

        viewModel.history
            .flowWithLifecycle(viewLifecycleOwner.lifecycle)
            .onEach {
                adapter.clear()
                adapter.addAll(it)
                adapter.notifyDataSetChanged()
            }
            .launchIn(viewLifecycleOwner.lifecycleScope)

        builder.setNegativeButton("キャンセル") { _, _ ->
            dismiss()
        }

        return builder.create()
    }

    class HistoryAdapter(
        context: Context,
        lines: List<AppRebootLog>
    ) : ArrayAdapter<AppRebootLog>(context, 0, lines) {

        private val inflater = LayoutInflater.from(context)

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = convertView ?: kotlin.run {
                inflater.inflate(R.layout.cell_list_history, parent, false)
            }
            getItem(position)?.let { log ->
                view.findViewById<TextView>(R.id.text_history_time).text = formatTime(
                    TIME_PATTERN_DATETIME, log.start
                )
                view.findViewById<TextView>(R.id.text_history_duration).text = log.finish?.let {
                    val duration = ((it.time - log.start.time) / 1000L).toInt()
                    formatTime(context, duration)
                } ?: ""
                view.findViewById<View>(R.id.text_history_now).visibility =
                    if (position == 0) View.VISIBLE else View.GONE
                view.findViewById<View>(R.id.text_history_error).visibility =
                    if (log.error) View.VISIBLE else View.GONE
            }
            return view
        }
    }
}

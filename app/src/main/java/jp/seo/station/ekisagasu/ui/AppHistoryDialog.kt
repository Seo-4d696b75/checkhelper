package jp.seo.station.ekisagasu.ui

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import dagger.hilt.android.AndroidEntryPoint
import jp.seo.station.ekisagasu.R
import jp.seo.station.ekisagasu.core.AppRebootLog
import jp.seo.station.ekisagasu.core.StationRepository
import jp.seo.station.ekisagasu.core.UserRepository
import jp.seo.station.ekisagasu.utils.TIME_PATTERN_DATETIME
import jp.seo.station.ekisagasu.utils.formatTime
import jp.seo.station.ekisagasu.viewmodel.ActivityViewModel
import javax.inject.Inject

/**
 * @author Seo-4d696b75
 * @version 2021/02/04.
 */
@AndroidEntryPoint
class AppHistoryDialog : DialogFragment() {

    companion object {
        fun getInstance(): AppHistoryDialog {
            return AppHistoryDialog()
        }

        const val DIALOG_SELECT_HISTORY = "select_history"
    }

    @Inject
    lateinit var stationRepository: StationRepository

    @Inject
    lateinit var userRepository: UserRepository

    private val viewModel: ActivityViewModel by lazy {
        ActivityViewModel.getInstance(
            requireActivity(),
            requireContext(),
            stationRepository,
            userRepository
        )
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val ctx = requireContext()
        val builder = AlertDialog.Builder(ctx)

        builder.setTitle(ctx.getString(R.string.dialog_title_history))

        val view = View.inflate(ctx, R.layout.dialog_history, null)
        builder.setView(view)
        val list = view.findViewById<ListView>(R.id.list_reboot_history)
        viewModel.histories.observe(this) {
            val adapter = HistoryAdapter(requireContext(), it)
            list.adapter = adapter
            list.setOnItemClickListener { parent, view, position, id ->
                viewModel.setLogTarget(it[position])
                dismiss()
            }
        }

        builder.setNegativeButton("キャンセル") { dialog, which ->
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
                    val duration = (it.time - log.start.time) / (1000L * 60L)
                    if (duration < 60) {
                        "${duration}min"
                    } else {
                        String.format("%f.1h", duration.toFloat() / 60f)
                    }
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

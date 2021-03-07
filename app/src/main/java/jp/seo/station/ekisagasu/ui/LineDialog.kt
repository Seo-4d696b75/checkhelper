package jp.seo.station.ekisagasu.ui

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ListView
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelStore
import dagger.hilt.android.AndroidEntryPoint
import jp.seo.station.ekisagasu.Line
import jp.seo.station.ekisagasu.R
import jp.seo.station.ekisagasu.core.GPSClient
import jp.seo.station.ekisagasu.core.NavigationRepository
import jp.seo.station.ekisagasu.core.StationRepository
import jp.seo.station.ekisagasu.core.UserRepository
import jp.seo.station.ekisagasu.viewmodel.ActivityViewModel
import jp.seo.station.ekisagasu.viewmodel.ApplicationViewModel
import java.util.*
import javax.inject.Inject

/**
 * @author Seo-4d696b75
 * @version 2021/01/24.
 */
@AndroidEntryPoint
class LineDialog : DialogFragment() {

    companion object {
        fun getInstance(): LineDialog {
            return LineDialog()
        }

        const val DIALOG_SELECT_CURRENT = "select_line"
        const val DIALOG_SELECT_NAVIGATION = "select_navigation_line"
    }

    @Inject
    lateinit var stationRepository: StationRepository

    @Inject
    lateinit var userRepository: UserRepository

    @Inject
    lateinit var gpsClient: GPSClient

    @Inject
    lateinit var navigator: NavigationRepository

    @Inject
    lateinit var singletonStore: ViewModelStore

    private val viewModel: ActivityViewModel by lazy {
        ActivityViewModel.getInstance(
            requireActivity(),
            requireContext(),
            stationRepository,
            userRepository
        )
    }

    private val appViewModel: ApplicationViewModel by lazy {
        ApplicationViewModel.getInstance(
            { singletonStore },
            stationRepository,
            userRepository,
            gpsClient,
            navigator
        )
    }

    private var type: String = ""

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val ctx = requireContext()
        val builder = AlertDialog.Builder(ctx)

        builder.setTitle(ctx.getString(R.string.dialog_title_select_line))

        val view = View.inflate(ctx, R.layout.dialog_select_line, null)
        builder.setView(view)

        val message = view.findViewById<TextView>(R.id.text_select_line_message)
        type = viewModel.dialogType

        when (type) {
            DIALOG_SELECT_CURRENT -> {
                message.text = ctx.getString(R.string.dialog_message_select_line)
                if (appViewModel.selectedLine.value != null) {
                    builder.setPositiveButton("解除") { dialog, which ->
                        appViewModel.selectLine(null)
                    }
                }
            }
            DIALOG_SELECT_NAVIGATION -> {
                message.text = ctx.getString(R.string.dialog_message_select_prediction)
                if (appViewModel.isNavigationRunning.value == true) {
                    builder.setPositiveButton("解除") { dialog, which ->
                        appViewModel.setNavigationLine(null)
                    }
                }
            }
            else -> {
                Log.w("LineDialog", "unexpected dialog type: ${viewModel.dialogType}")
            }
        }

        builder.setNeutralButton("キャンセル") { dialog, which ->
            dismiss()
        }

        val listView = view.findViewById<ListView>(R.id.list_near_lines)
        stationRepository.nearestStations.value?.let { list ->
            val lines: MutableList<Line> = LinkedList()
            list.forEach { n ->
                n.lines.forEach {
                    if (!lines.contains(it)) lines.add(it)
                }
            }
            listView.adapter = StationFragment.LineAdapter(ctx, lines)
            listView.setOnItemClickListener { parent, view, position, id ->
                onLineSelected(lines[position])
            }
        }

        return builder.create()
    }

    private fun onLineSelected(line: Line) {
        when (type) {
            DIALOG_SELECT_CURRENT -> {
                appViewModel.selectLine(line)
                dismiss()
            }
            DIALOG_SELECT_NAVIGATION -> {
                appViewModel.setNavigationLine(line)
                dismiss()
            }
            else -> {
                Log.w("LineDialog", "unexpected dialog type: ${viewModel.dialogType}")
            }
        }
    }

}

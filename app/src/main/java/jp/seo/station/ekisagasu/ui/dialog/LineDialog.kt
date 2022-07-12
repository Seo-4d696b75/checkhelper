package jp.seo.station.ekisagasu.ui.dialog

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import dagger.hilt.android.AndroidEntryPoint
import jp.seo.station.ekisagasu.R
import jp.seo.station.ekisagasu.databinding.DialogSelectLineBinding
import jp.seo.station.ekisagasu.ui.common.LineAdapter

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
    }

    private val viewModel: LineSelectionViewModel by viewModels()

    private val type: LineDialogType by navArgs()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val context = requireContext()
        val builder = AlertDialog.Builder(context)

        builder.setTitle(context.getString(R.string.dialog_title_select_line))

        val binding = DataBindingUtil.inflate<DialogSelectLineBinding>(
            layoutInflater,
            R.layout.dialog_select_line,
            null,
            false,
        )

        viewModel.setUiState(context, type)
        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner
        builder.setView(binding.root)

        when (type) {
            LineDialogType.SELECT_CURRENT -> {
                if (viewModel.currentLine != null) {
                    builder.setPositiveButton(R.string.dialog_button_unregister) { _, _ ->
                        viewModel.selectCurrentLine(null)
                    }
                }
            }
            LineDialogType.SELECT_NAVIGATION -> {
                if (viewModel.isNavigationRunning) {
                    builder.setPositiveButton(R.string.dialog_button_unregister) { _, _ ->
                        viewModel.selectNavigationLine(null)
                    }
                }
            }
        }

        builder.setNeutralButton(R.string.dialog_button_cancel) { _, _ ->
            dismiss()
        }

        binding.listNearLines.also {
            val lines = viewModel.lines
            it.adapter = LineAdapter(context, lines)
            it.setOnItemClickListener { _, _, position, _ ->
                viewModel.onLineSelected(lines[position])
                dismiss()
            }
        }

        return builder.create()
    }
}

enum class LineDialogType {
    SELECT_CURRENT,
    SELECT_NAVIGATION,
}
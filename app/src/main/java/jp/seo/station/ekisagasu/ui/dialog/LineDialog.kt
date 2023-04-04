package jp.seo.station.ekisagasu.ui.dialog

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import dagger.hilt.android.AndroidEntryPoint
import jp.seo.station.ekisagasu.R
import jp.seo.station.ekisagasu.databinding.DialogSelectLineBinding
import jp.seo.station.ekisagasu.ui.common.LineAdapter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/**
 * @author Seo-4d696b75
 * @version 2021/01/24.
 */
@AndroidEntryPoint
class LineDialog : DialogFragment() {

    private val viewModel: LineSelectionViewModel by viewModels()

    private val args: LineDialogArgs by navArgs()

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

        binding.viewModel = viewModel
        builder.setView(binding.root)

        when (args.type) {
            LineDialogType.Current -> {
                if (viewModel.currentLine != null) {
                    builder.setPositiveButton(R.string.dialog_button_unregister) { _, _ ->
                        viewModel.selectCurrentLine(null)
                    }
                }
            }
            LineDialogType.Navigation -> {
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

        viewModel
            .event
            .flowWithLifecycle(lifecycle)
            .onEach {
                when (it) {
                    is LineSelectionViewModel.Event.Error -> {
                        Toast.makeText(
                            requireActivity(),
                            requireContext().getString(it.message),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
            .launchIn(lifecycleScope)

        return builder.create()
    }
}

enum class LineDialogType {
    Current,
    Navigation,
}

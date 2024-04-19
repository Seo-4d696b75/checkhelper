package com.seo4d696b75.android.ekisagasu.ui.top.line

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
import com.seo4d696b75.android.ekisagasu.ui.R
import dagger.hilt.android.AndroidEntryPoint
import com.seo4d696b75.android.ekisagasu.ui.common.LineAdapter
import com.seo4d696b75.android.ekisagasu.ui.databinding.DialogSelectLineBinding
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.take

/**
 * @author Seo-4d696b75
 * @version 2021/01/24.
 */
@AndroidEntryPoint
class LineSelectDialog : DialogFragment() {
    private val viewModel: LineSelectionViewModel by viewModels()

    private val args: LineSelectDialogArgs by navArgs()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val context = requireContext()
        val builder = AlertDialog.Builder(context)

        builder.setTitle(context.getString(R.string.dialog_title_select_line))

        val binding = DialogSelectLineBinding.inflate(layoutInflater)

        binding.viewModel = viewModel
        builder.setView(binding.root)

        when (args.type) {
            LineSelectType.Current -> {
                if (viewModel.currentLine != null) {
                    builder.setPositiveButton(R.string.dialog_button_unregister) { _, _ ->
                        viewModel.selectCurrentLine(null)
                    }
                }
            }

            LineSelectType.Navigation -> {
                if (viewModel.navigatorLine != null) {
                    builder.setPositiveButton(R.string.dialog_button_unregister) { _, _ ->
                        viewModel.selectNavigationLine(null)
                    }
                }
            }
        }

        builder.setNeutralButton(R.string.dialog_button_cancel) { _, _ ->
            dismiss()
        }

        viewModel
            .lines
            .flowWithLifecycle(lifecycle)
            .take(1)
            .onEach { lines ->
                binding.listNearLines.also {
                    it.adapter = LineAdapter(context, lines)
                    it.setOnItemClickListener { _, _, position, _ ->
                        viewModel.onLineSelected(lines[position])
                        dismiss()
                    }
                }
            }
            .launchIn(lifecycleScope)

        viewModel
            .event
            .flowWithLifecycle(lifecycle)
            .onEach {
                when (it) {
                    is LineSelectionViewModel.Event.Error -> {
                        Toast.makeText(
                            requireActivity(),
                            requireContext().getString(it.message),
                            Toast.LENGTH_SHORT,
                        ).show()
                    }
                }
            }
            .launchIn(lifecycleScope)

        return builder.create()
    }
}


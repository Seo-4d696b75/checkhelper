package com.seo4d696b75.android.ekisagasu.ui.log.output

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import com.seo4d696b75.android.ekisagasu.ui.R
import com.seo4d696b75.android.ekisagasu.ui.databinding.DialogLogOutputConfigBinding
import com.seo4d696b75.android.ekisagasu.ui.log.LogViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class LogOutputConfDialog : DialogFragment() {
    private val viewModel: LogOutputConfViewModel by viewModels()
    private val logViewModel: LogViewModel by activityViewModels()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val binding = DialogLogOutputConfigBinding.inflate(layoutInflater)
        binding.radioGroupFileExtension.setOnCheckedChangeListener { group, checkedId ->
            viewModel.onChecked(checkedId)
        }
        binding.viewModel = viewModel

        return AlertDialog.Builder(requireContext()).apply {
            setTitle(R.string.dialog_log_output_config_title)
            setPositiveButton(R.string.dialog_button_positive) { _, _ ->
                logViewModel.requestLogOutput(viewModel.currentConfig)
                dismiss()
            }
            setNegativeButton(R.string.dialog_button_negative) { _, _ ->
                dismiss()
            }
            setView(binding.root)
        }.create().apply {
            setCanceledOnTouchOutside(false)
        }
    }
}

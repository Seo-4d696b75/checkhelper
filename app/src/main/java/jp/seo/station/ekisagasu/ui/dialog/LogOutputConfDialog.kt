package jp.seo.station.ekisagasu.ui.dialog

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import jp.seo.station.ekisagasu.R
import jp.seo.station.ekisagasu.databinding.DialogLogOutputConfigBinding

@AndroidEntryPoint
class LogOutputConfDialog : DialogFragment() {

    val viewModel: LogOutputConfViewModel by viewModels()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val binding = DataBindingUtil.inflate<DialogLogOutputConfigBinding>(
            layoutInflater,
            R.layout.dialog_log_output_config,
            null,
            false,
        )
        binding.radioGroupFileExtension.setOnCheckedChangeListener { group, checkedId ->
            viewModel.onChecked(checkedId)
        }
        binding.viewModel = viewModel

        return AlertDialog.Builder(requireContext()).apply {
            setTitle(R.string.dialog_log_output_config_title)
            setPositiveButton(R.string.dialog_button_positive) { _, _ ->
                viewModel.writeLog()
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

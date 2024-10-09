package com.seo4d696b75.android.ekisagasu.ui.update

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import com.seo4d696b75.android.ekisagasu.domain.dataset.update.DataUpdateType
import com.seo4d696b75.android.ekisagasu.ui.R
import com.seo4d696b75.android.ekisagasu.ui.databinding.DialogDataCheckBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ConfirmDataUpdateDialog : DialogFragment() {
    private val args: ConfirmDataUpdateDialogArgs by navArgs()

    private val viewModel: ConfirmDataUpdateViewModel by viewModels()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val binding = DialogDataCheckBinding.inflate(layoutInflater)
        binding.viewModel = viewModel

        return AlertDialog.Builder(context).apply {
            setTitle(
                if (args.type == DataUpdateType.Init) {
                    R.string.dialog_title_init_data
                } else {
                    R.string.dialog_title_latest_data
                },
            )
            setPositiveButton(R.string.dialog_button_positive) { _, _ ->
                viewModel.onResult(true)
                dismiss()
            }
            setNegativeButton(R.string.dialog_button_negative) { _, _ ->
                viewModel.onResult(false)
                dismiss()
            }
            setView(binding.root)
        }.create().apply {
            setCanceledOnTouchOutside(false)
        }
    }
}

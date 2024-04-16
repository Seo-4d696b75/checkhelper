package jp.seo.station.ekisagasu.ui.permission

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.navArgs
import dagger.hilt.android.AndroidEntryPoint
import jp.seo.station.ekisagasu.R
import jp.seo.station.ekisagasu.databinding.DialogPermissionRationaleBinding
import javax.inject.Inject

@AndroidEntryPoint
class PermissionRationaleDialog @Inject constructor() : DialogFragment() {

    private val args: PermissionRationaleDialogArgs by navArgs()
    private val rationale: PermissionRationale by lazy {
        args.rationale.rationale
    }

    private val viewModel: PermissionViewModel by activityViewModels()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val binding = DialogPermissionRationaleBinding.inflate(layoutInflater)
        binding.rationale = rationale

        return AlertDialog.Builder(context).apply {
            setTitle(R.string.dialog_title_permission)
            setPositiveButton(R.string.dialog_button_next) { _, _ ->
                viewModel.requestPermission(rationale)
                dismiss()
            }
            setNegativeButton(R.string.dialog_button_finish_app) { _, _ ->
                viewModel.onPermissionRequestCancelled()
                dismiss()
            }
            setView(binding.root)
        }.create().apply {
            setCancelable(false)
            setCanceledOnTouchOutside(false)
        }
    }
}

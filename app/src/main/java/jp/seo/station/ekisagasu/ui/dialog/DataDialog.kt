package jp.seo.station.ekisagasu.ui.dialog

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.view.View
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import androidx.navigation.fragment.navArgs
import dagger.hilt.android.AndroidEntryPoint
import jp.seo.station.ekisagasu.R
import jp.seo.station.ekisagasu.api.DataLatestInfo
import jp.seo.station.ekisagasu.databinding.DialogDataCheckBinding
import jp.seo.station.ekisagasu.databinding.DialogDataUpdateBinding
import jp.seo.station.ekisagasu.usecase.DataUpdateResult
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

enum class DataUpdateType {
    Init,
    Latest,
}

@AndroidEntryPoint
class ConfirmDataUpdateDialog : DialogFragment() {

    private val args: ConfirmDataUpdateDialogArgs by navArgs()

    private val viewModel: ConfirmDataUpdateViewModel by viewModels()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        val binding = DataBindingUtil.inflate<DialogDataCheckBinding>(
            layoutInflater,
            R.layout.dialog_data_check,
            null,
            false,
        )
        binding.viewModel = viewModel

        return AlertDialog.Builder(context).apply {
            setTitle(if (args.type == DataUpdateType.Init) R.string.dialog_title_init_data else R.string.dialog_title_latest_data)
            setPositiveButton(R.string.dialog_button_update_positive) { _, _ ->
                viewModel.onResult(true)
                dismiss()
            }
            setNegativeButton(R.string.dialog_button_update_negative) { _, _ ->
                viewModel.onResult(false)
                dismiss()
            }
            setView(binding.root)
        }.create().apply {
            setCanceledOnTouchOutside(false)
        }
    }

}

@AndroidEntryPoint
class DataUpdateDialog : DialogFragment() {

    private val type: DataUpdateType by navArgs()

    private val info: DataLatestInfo by lazy {
        arguments?.getSerializable("info") as DataLatestInfo
    }

    private val viewModel: DataUpdateViewModel by viewModels()

    private lateinit var binding: DialogDataUpdateBinding

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        viewModel.setTargetData(type, info)

        binding = DataBindingUtil.inflate(
            layoutInflater,
            R.layout.dialog_data_update,
            null,
            false,
        )

        // 更新が終わったら閉じる
        viewModel.result
            .flowWithLifecycle(viewLifecycleOwner.lifecycle)
            .onEach {
                when (it) {
                    is DataUpdateResult.Success -> {
                        viewModel.onResult(true)
                    }
                    is DataUpdateResult.Failure -> {
                        viewModel.onResult(false)
                    }
                }
                dismiss()
            }
            .launchIn(viewLifecycleOwner.lifecycleScope)

        return AlertDialog.Builder(context).apply {
            setTitle(R.string.dialog_title_update_data)
            setNegativeButton(R.string.dialog_button_update_negative) { _, _ ->
                viewModel.viewModelScope.coroutineContext.cancel()
                viewModel.onResult(false)
                dismiss()
            }
            setView(binding.root)
        }.create().apply {
            setCanceledOnTouchOutside(false)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner

        viewModel.update()
    }

}

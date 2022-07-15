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

    private val type: DataUpdateType by navArgs()

    private val info: DataLatestInfo by lazy {
        requireArguments().getSerializable("info") as DataLatestInfo
    }

    private val viewModel: ConfirmDataUpdateViewModel by viewModels()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        viewModel.setTargetData(type, info)

        val binding = DataBindingUtil.inflate<DialogDataCheckBinding>(
            layoutInflater,
            R.layout.dialog_data_check,
            null,
            false,
        ).also {
            it.viewModel = viewModel
            it.lifecycleOwner = viewLifecycleOwner
        }

        return AlertDialog.Builder(context).apply {
            setTitle(if (type == DataUpdateType.Init) R.string.dialog_title_init_data else R.string.dialog_title_latest_data)
            setPositiveButton(R.string.dialog_button_update_positive) { _, _ ->
                // TODO 結果の通知
                dismiss()
            }
            setNegativeButton(R.string.dialog_button_update_negative) { _, _ ->
                // TODO 結果の通知
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

    private val info: DataLatestInfo by lazy {
        arguments?.getSerializable("info") as DataLatestInfo
    }
    private val viewModel: DataUpdateViewModel by viewModels()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        val binding = DataBindingUtil.inflate<DialogDataUpdateBinding>(
            layoutInflater,
            R.layout.dialog_data_update,
            null,
            false,
        )
        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner

        // 更新が終わったら閉じる
        viewModel.result
            .flowWithLifecycle(viewLifecycleOwner.lifecycle)
            .onEach {
                when (it) {
                    // TODO 結果の通知
                    is DataUpdateResult.Success -> {

                    }
                    is DataUpdateResult.Failure -> {

                    }
                }
                dismiss()
            }
            .launchIn(viewLifecycleOwner.lifecycleScope)

        return AlertDialog.Builder(context).apply {
            setTitle(R.string.dialog_title_update_data)
            setNegativeButton(R.string.dialog_button_update_negative) { _, _ ->
                viewModel.viewModelScope.coroutineContext.cancel()
                // TODO 結果の通知
                dismiss()
            }
            setView(binding.root)
        }.create().apply {
            setCanceledOnTouchOutside(false)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.update(info)
    }

}

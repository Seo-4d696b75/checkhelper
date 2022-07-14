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

enum class DataCheckType {
    Init,
    Latest,
}

@AndroidEntryPoint
class DataCheckDialog : DialogFragment() {

    private val type: DataCheckType by navArgs()

    private val info: DataLatestInfo by navArgs()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        val binding = DataBindingUtil.inflate<DialogDataCheckBinding>(
            layoutInflater,
            R.layout.dialog_data_check,
            null,
            false,
        ).apply {
            textVersion.text = getString(R.string.text_data_version, info.version)
            textSize.text = getString(R.string.text_data_size, info.fileSize())
            textDialogMessage.text = getString(
                if (type == DataCheckType.Init)
                    R.string.dialog_message_init_data
                else
                    R.string.dialog_message_latest_data
            )
        }

        return AlertDialog.Builder(context).apply {
            setTitle(if (type == DataCheckType.Init) R.string.dialog_title_init_data else R.string.dialog_title_latest_data)
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

    private val info: DataLatestInfo by navArgs()

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

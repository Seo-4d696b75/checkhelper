package com.seo4d696b75.android.ekisagasu.ui.update

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import com.seo4d696b75.android.ekisagasu.ui.R
import com.seo4d696b75.android.ekisagasu.ui.databinding.DialogDataUpdateBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

@AndroidEntryPoint
class DataUpdateDialog : DialogFragment() {
    private val viewModel: DataUpdateViewModel by viewModels()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val binding = DialogDataUpdateBinding.inflate(layoutInflater)
        binding.viewModel = viewModel
        binding.lifecycleOwner = this

        // 更新が終わったら閉じる
        viewModel.result
            .flowWithLifecycle(lifecycle)
            .onEach {
                dismiss()
            }
            .launchIn(lifecycleScope)

        // 実行
        viewModel.update()

        return AlertDialog.Builder(context).apply {
            setTitle(R.string.dialog_title_update_data)
            setNegativeButton(R.string.dialog_button_negative) { _, _ ->
                viewModel.viewModelScope.coroutineContext.cancel()
                viewModel.onCancel()
                dismiss()
            }
            setView(binding.root)
        }.create().apply {
            setCanceledOnTouchOutside(false)
        }
    }
}

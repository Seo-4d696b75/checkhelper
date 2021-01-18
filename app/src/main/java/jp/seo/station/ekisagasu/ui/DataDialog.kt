package jp.seo.station.ekisagasu.ui

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import dagger.hilt.android.AndroidEntryPoint
import jp.seo.station.ekisagasu.R
import jp.seo.station.ekisagasu.core.StationRepository
import jp.seo.station.ekisagasu.core.StationRepository.UpdateProgressListener
import jp.seo.station.ekisagasu.core.UserRepository
import jp.seo.station.ekisagasu.viewmodel.ActivityViewModel
import javax.inject.Inject

/**
 * @author Seo-4d696b75
 * @version 2021/01/07.
 */
@AndroidEntryPoint
open class DataDialog : DialogFragment() {


    companion object {
        const val DIALOG_INIT = "dialog_init_data"
        const val DIALOG_LATEST = "dialog_latest_data"
        const val DIALOG_UPDATE = "dialog_update"

    }

    @Inject
    lateinit var stationRepository: StationRepository

    @Inject
    lateinit var userRepository: UserRepository

    val viewModel: ActivityViewModel by lazy {
        ActivityViewModel.getInstance(
            requireActivity(),
            requireActivity(),
            stationRepository,
            userRepository
        )
    }

    final override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        val builder = AlertDialog.Builder(context)

        onCreateDialog(builder)

        return builder.create().apply {
            setCanceledOnTouchOutside(false)
        }
    }

    open fun onCreateDialog(builder: AlertDialog.Builder) {}

}

@AndroidEntryPoint
class DataCheckDialog : DataDialog() {

    override fun onCreateDialog(builder: AlertDialog.Builder) {

        val type = viewModel.dialogType
        context?.let { ctx ->
            viewModel.targetInfo?.let { info ->
                val init = (type == DIALOG_INIT)

                builder.setTitle(if (init) R.string.dialog_title_init_data else R.string.dialog_title_latest_data)

                val inflater = LayoutInflater.from(ctx)

                val view = inflater.inflate(R.layout.dialog_data_check, null, false)
                view.findViewById<TextView>(R.id.text_version).text =
                    String.format("version: %d", info.version)
                view.findViewById<TextView>(R.id.text_size).text =
                    String.format("size: %s", info.fileSize())
                view.findViewById<TextView>(R.id.text_dialog_message).text =
                    ctx.getString(if (init) R.string.dialog_message_init_data else R.string.dialog_message_latest_data)
                builder.setView(view)

                builder.setPositiveButton("OK") { dialog, id ->
                    viewModel.handleDialogResult(type, info, true)
                    dismiss()
                }
            }

        }

        builder.setNegativeButton("Cancel") { dialog, id ->
            viewModel.handleDialogResult(type, viewModel.targetInfo, false)
            dismiss()
        }
    }

}

@AndroidEntryPoint
class DataUpdateDialog : DataDialog() {

    override fun onCreateDialog(builder: AlertDialog.Builder) {
        val type = viewModel.dialogType
        context?.let { ctx ->
            viewModel.targetInfo?.let { info ->
                builder.setTitle(R.string.dialog_title_update_data)

                val inflater = LayoutInflater.from(ctx)
                val view = inflater.inflate(R.layout.dialog_data_update, null, false)
                builder.setView(view)

                val progress = view.findViewById<ProgressBar>(R.id.progress_update)
                val state = view.findViewById<TextView>(R.id.text_update_state)
                val percent = view.findViewById<TextView>(R.id.text_update_progress)

                viewModel.updateState.observe(this) {
                    state.text = parseUpdateState(it)
                }
                viewModel.updateProgress.observe(this) {
                    percent.text = String.format("%d%%", it)
                    progress.progress = it
                }

                viewModel.updateStationData(info) { result ->
                    viewModel.handleDialogResult(type, info, true)
                    dismiss()
                }
            }
        }

        builder.setNegativeButton("Cancel") { dialog, id ->
            viewModel.handleDialogResult(type, viewModel.targetInfo, false)
            dismiss()
        }
    }

    private fun parseUpdateState(state: String): String {
        return context?.let {
            when (state) {
                UpdateProgressListener.STATE_DOWNLOAD -> return@let it.getString(R.string.update_state_download)
                UpdateProgressListener.STATE_CLEAN -> return@let it.getString(R.string.update_state_clean)
                UpdateProgressListener.STATE_PARSE -> return@let it.getString(R.string.update_state_parse)
                UpdateProgressListener.STATE_ADD -> return@let it.getString(R.string.update_state_add)
                else -> return@let ""
            }
        } ?: ""
    }

}

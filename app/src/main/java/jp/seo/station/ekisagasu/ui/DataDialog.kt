package jp.seo.station.ekisagasu.ui

import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint
import jp.seo.station.ekisagasu.R
import jp.seo.station.ekisagasu.core.DataLatestInfo
import jp.seo.station.ekisagasu.core.StationRepository
import jp.seo.station.ekisagasu.core.StationRepository.UpdateProgressListener
import jp.seo.station.ekisagasu.utils.getViewModelFactory
import jp.seo.station.ekisagasu.viewmodel.DataCheckViewModel
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

        const val KEY_INFO = "data_info"
        const val KEY_TYPE = "data_type"
    }

    interface OnClickListener {
        fun onDialogButtonClicked(tag: String?, info: DataLatestInfo, which: Int)
    }

    var listener: OnClickListener? = null

    val viewModel: DataCheckViewModel by lazy {
        getViewModelFactory {
            val args = arguments ?: throw IllegalArgumentException("args not found")
            DataCheckViewModel(args)
        }.create(DataCheckViewModel::class.java)
    }

    final override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val fragment = targetFragment
        val activity = this.activity
        if (fragment is OnClickListener) {
            listener = fragment
        } else if (activity is OnClickListener) {
            listener = activity
        }


        val builder = AlertDialog.Builder(context)

        onCreateDialog(builder)

        return builder.create()
    }

    open fun onCreateDialog(builder: AlertDialog.Builder) {}

}

@AndroidEntryPoint
class DataCheckDialog : DataDialog() {

    companion object {

        fun getInstance(info: DataLatestInfo, init: Boolean): DataCheckDialog {
            val dialog = DataCheckDialog()
            val args = Bundle()
            args.putString(KEY_INFO, Gson().toJson(info))
            args.putString(KEY_TYPE, if (init) DIALOG_INIT else DIALOG_LATEST)
            dialog.arguments = args
            return dialog
        }

    }

    override fun onCreateDialog(builder: AlertDialog.Builder) {

        context?.let { ctx ->

            val init = (viewModel.type == DIALOG_INIT)

            builder.setTitle(if (init) R.string.dialog_title_init_data else R.string.dialog_title_latest_data)

            val inflater = LayoutInflater.from(ctx)

            val view = inflater.inflate(R.layout.dialog_data_check, null, false)
            view.findViewById<TextView>(R.id.text_version).text =
                String.format("version: %d", viewModel.info.version)
            view.findViewById<TextView>(R.id.text_size).text =
                String.format("size: %s", viewModel.info.fileSize())
            view.findViewById<TextView>(R.id.text_dialog_message).text =
                ctx.getString(if (init) R.string.dialog_message_init_data else R.string.dialog_message_latest_data)
            builder.setView(view)

            builder.setPositiveButton("OK") { dialog, id ->
                listener?.onDialogButtonClicked(
                    tag,
                    viewModel.info,
                    DialogInterface.BUTTON_POSITIVE
                )
                dismiss()
            }

            builder.setNegativeButton("Cancel") { dialog, id ->
                listener?.onDialogButtonClicked(
                    tag,
                    viewModel.info,
                    DialogInterface.BUTTON_NEGATIVE
                )
                dismiss()
            }
        }
    }

}

@AndroidEntryPoint
class DataUpdateDialog : DataDialog() {

    companion object {

        fun getInstance(info: DataLatestInfo): DataUpdateDialog {
            val dialog = DataUpdateDialog()
            val args = Bundle()
            args.putString(KEY_INFO, Gson().toJson(info))
            args.putString(KEY_TYPE, DIALOG_UPDATE)
            dialog.arguments = args
            return dialog
        }

    }

    @Inject
    lateinit var stationRepository: StationRepository

    override fun onCreateDialog(builder: AlertDialog.Builder) {
        context?.let { ctx ->

            builder.setTitle(R.string.dialog_title_update_data)

            val inflater = LayoutInflater.from(ctx)
            val view = inflater.inflate(R.layout.dialog_data_update, null, false)
            builder.setView(view)

            builder.setNegativeButton("Cancel") { dialog, id ->
                listener?.onDialogButtonClicked(
                    tag,
                    viewModel.info,
                    DialogInterface.BUTTON_NEGATIVE
                )
                dismiss()
            }

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

            viewModel.updateStationData(stationRepository) { result ->
                listener?.onDialogButtonClicked(
                    tag,
                    viewModel.info,
                    if (result) DialogInterface.BUTTON_POSITIVE else DialogInterface.BUTTON_NEGATIVE
                )
                dismiss()
            }

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

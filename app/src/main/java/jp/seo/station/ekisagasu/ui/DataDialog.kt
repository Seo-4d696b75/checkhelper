package jp.seo.station.ekisagasu.ui

import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.google.gson.Gson
import jp.seo.station.ekisagasu.R
import jp.seo.station.ekisagasu.core.DataLatestInfo
import jp.seo.station.ekisagasu.utils.getViewModelFactory
import jp.seo.station.ekisagasu.viewmodel.DataCheckViewModel
import org.w3c.dom.Text
import java.lang.IllegalArgumentException

/**
 * @author Seo-4d696b75
 * @version 2021/01/07.
 */
abstract class DataDialog : DialogFragment() {


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

    var _listener: OnClickListener? = null

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
            _listener = fragment
        } else if (activity is OnClickListener) {
            _listener = activity
        }

        val builder = AlertDialog.Builder(context)

        onCreateDialog(builder)

        return builder.create()
    }

    abstract fun onCreateDialog(builder: AlertDialog.Builder)

}

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
                String.format("size: %s", viewModel.info.fileSize)
            view.findViewById<TextView>(R.id.text_dialog_message).text =
                ctx.getString(if (init) R.string.dialog_message_init_data else R.string.dialog_message_latest_data)
            builder.setView(view)

            builder.setPositiveButton("OK") { dialog, id ->
                _listener?.onDialogButtonClicked(
                    tag,
                    viewModel.info,
                    DialogInterface.BUTTON_POSITIVE
                )
                dismiss()
            }

            builder.setNegativeButton("Cancel") { dialog, id ->
                _listener?.onDialogButtonClicked(
                    tag,
                    viewModel.info,
                    DialogInterface.BUTTON_NEGATIVE
                )
                dismiss()
            }
        }
    }

}

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

    override fun onCreateDialog(builder: AlertDialog.Builder) {
        builder.setTitle(R.string.dialog_title_update_data)
        builder.setMessage("TODO")

        builder.setNegativeButton("Cancel") { dialog, id ->
            _listener?.onDialogButtonClicked(tag, viewModel.info, DialogInterface.BUTTON_NEGATIVE)
            dismiss()
        }
    }

}

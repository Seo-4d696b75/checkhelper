package jp.seo.station.ekisagasu.viewmodel

import android.os.Bundle
import androidx.lifecycle.ViewModel
import com.google.gson.Gson
import jp.seo.station.ekisagasu.core.DataLatestInfo
import jp.seo.station.ekisagasu.ui.DataDialog

/**
 * @author Seo-4d696b75
 * @version 2021/01/07.
 */
class DataCheckViewModel(
    args: Bundle
) : ViewModel() {

    val type: String
    val info: DataLatestInfo


    init {
        type = args.getString(DataDialog.KEY_TYPE, "none")
        val data = args.getString(DataDialog.KEY_INFO)
        info = Gson().fromJson(data, DataLatestInfo::class.java)
    }

}

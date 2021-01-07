package jp.seo.station.ekisagasu.viewmodel

import android.os.Bundle
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import jp.seo.station.ekisagasu.core.DataLatestInfo
import jp.seo.station.ekisagasu.core.StationRepository
import jp.seo.station.ekisagasu.ui.DataDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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

    private val _updateState = MutableLiveData<String>("")
    private val _updateProgress = MutableLiveData<Int>(0)

    val updateState: LiveData<String>
        get() = _updateState

    val updateProgress: LiveData<Int>
        get() = _updateProgress

    fun updateStationData(repository: StationRepository, callback: (Boolean)->Unit){
        viewModelScope.launch {
            withContext(Dispatchers.IO){
                repository.updateData(info.version, info.url, object : StationRepository.UpdateProgressListener{
                    override fun onStateChanged(state: String) {
                        _updateState.value = state
                    }

                    override fun onProgress(progress: Int) {
                        _updateProgress.value = progress
                    }

                    override fun onComplete(success: Boolean) {
                        callback(success)
                    }

                })
            }
        }
    }

}

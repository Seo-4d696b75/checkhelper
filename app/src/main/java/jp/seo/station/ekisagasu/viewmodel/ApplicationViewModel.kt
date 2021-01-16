package jp.seo.station.ekisagasu.viewmodel

import androidx.annotation.MainThread
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import com.google.android.gms.common.api.ResolvableApiException
import jp.seo.station.ekisagasu.utils.getViewModelFactory

/**
 * ApplicationComponentでInjectするViewModel(SingletonScoped)
 * @author Seo-4d696b75
 * @version 2021/01/16.
 */
class ApplicationViewModel : ViewModel() {

    companion object {
        fun getInstance(owner: ViewModelStoreOwner): ApplicationViewModel {
            return ViewModelProvider(owner, getViewModelFactory(::ApplicationViewModel)).get(
                ApplicationViewModel::class.java
            )
        }
    }

    val requestFinish = MutableLiveData<Boolean>(false)

    private val _running = MutableLiveData<Boolean>(false)

    /**
     * 探索が現在進行中であるか
     */
    val isRunning: LiveData<Boolean>
        get() = _running

    private val _requestRunning = MutableLiveData(false)
    val isRequestRunning: LiveData<Boolean> = _requestRunning

    @MainThread
    fun requestSearchRunning(running: Boolean) {
        _requestRunning.value = running
    }

    @MainThread
    fun setSearchState(running: Boolean) {
        _running.value = running
    }


    private val _apiException = MutableLiveData<ResolvableApiException?>(null)

    val apiException: LiveData<ResolvableApiException?>
        get() = _apiException

    @MainThread
    fun onApiException(e: ResolvableApiException) {
        _apiException.value = e
    }

    @MainThread
    fun onResolvedAPIException() {
        _apiException.value = null
    }
}

package jp.seo.station.ekisagasu.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import jp.seo.station.ekisagasu.core.AppLog
import jp.seo.station.ekisagasu.core.UserRepository
import jp.seo.station.ekisagasu.utils.combineLiveData
import jp.seo.station.ekisagasu.utils.getViewModelFactory

/**
 * @author Seo-4d696b75
 * @version 2020/12/21.
 */
class LogViewModel(
    repository: UserRepository
) : ViewModel() {

    companion object {
        fun getInstance(owner: ViewModelStoreOwner, repository: UserRepository): LogViewModel {
            return ViewModelProvider(owner, getViewModelFactory { LogViewModel(repository) }).get(
                LogViewModel::class.java
            )
        }
    }

    private var _filter: MutableLiveData<Int> = MutableLiveData(AppLog.FILTER_ALL)

    fun setFilter(value: Int) {
        _filter.value = value
    }

    val logs: LiveData<List<AppLog>> = combineLiveData(
        ArrayList(),
        _filter,
        repository.logs
    ) { filter, logs ->
        logs.filter { (it.type and filter) > 0 }
    }

}


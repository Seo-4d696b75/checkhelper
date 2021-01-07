package jp.seo.station.ekisagasu.viewmodel

import androidx.lifecycle.*
import jp.seo.station.ekisagasu.core.AppLog
import jp.seo.station.ekisagasu.core.UserRepository
import jp.seo.station.ekisagasu.utils.combine
import jp.seo.station.ekisagasu.utils.getViewModelFactory

/**
 * @author Seo-4d696b75
 * @version 2020/12/21.
 */
class LogViewModel(
    private val repository: UserRepository
) : ViewModel() {

    companion object {
        fun getFactory(repository: UserRepository): ViewModelProvider.Factory =
            getViewModelFactory {
                LogViewModel(repository)
            }
    }

    private var _filter: MutableLiveData<Int> = MutableLiveData(AppLog.FILTER_ALL)

    fun setFilter(value: Int){
        _filter.value = value
    }

    val logs: LiveData<List<AppLog>> = combine(
        ArrayList(),
        _filter,
        repository.logs
    ) { filter, logs ->
        logs.filter{ (it.type and filter) > 0 }
    }

}


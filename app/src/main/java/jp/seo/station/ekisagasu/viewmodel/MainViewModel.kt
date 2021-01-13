package jp.seo.station.ekisagasu.viewmodel


import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import jp.seo.station.ekisagasu.core.StationService
import jp.seo.station.ekisagasu.utils.getViewModelFactory

/**
 * @author Seo-4d696b75
 * @version 2020/12/16.
 */
class MainViewModel(
    private val service: StationService
) : ViewModel() {

    companion object {
        fun getFactory(service: StationService): ViewModelProvider.Factory =
            getViewModelFactory {
                MainViewModel(service)
            }
    }


    val running = service.isRunning

    fun toggleStart() {
        running.value?.let { state ->
            if (state) {
                service.stop()
            } else {
                service.start()
            }
        }
    }

}

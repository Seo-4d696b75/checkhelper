package jp.seo.station.ekisagasu.utils

import androidx.annotation.MainThread
import androidx.fragment.app.Fragment
import jp.seo.station.ekisagasu.core.StationService
import org.koin.android.ext.android.inject

/**
 * @author Seo-4d696b75
 * @version 2020/12/22.
 */
open class AppFragment : Fragment() {

    private val _service by inject<ServiceGetter>()

    @MainThread
    fun getService(block: (StationService) -> Unit) {
        _service.get(block)
    }
}

interface ActivityCallback {

}

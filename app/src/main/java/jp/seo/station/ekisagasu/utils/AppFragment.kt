package jp.seo.station.ekisagasu.utils

import android.content.Context
import androidx.annotation.MainThread
import androidx.fragment.app.Fragment
import jp.seo.station.ekisagasu.core.StationService
import jp.seo.station.ekisagasu.ui.MainActivity

/**
 * @author Seo-4d696b75
 * @version 2020/12/22.
 */
open class AppFragment : Fragment() {

    private var _service: ServiceGetter? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if ( context is MainActivity ){
            _service = context.service
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _service = null
    }

    @MainThread
    fun getService(block: (StationService) -> Unit){
        _service?.get(block)
    }
}

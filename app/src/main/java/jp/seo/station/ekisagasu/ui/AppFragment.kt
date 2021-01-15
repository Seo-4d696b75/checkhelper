package jp.seo.station.ekisagasu.ui

import androidx.annotation.MainThread
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import jp.seo.station.ekisagasu.core.StationService
import jp.seo.station.ekisagasu.utils.ServiceGetter
import org.koin.android.ext.android.inject

/**
 * アプリのUI実装に必要な共通機能を提供する基底クラス
 *
 * - [StationService] へアクセスするための機能
 * - [jp.seo.station.ekisagasu.viewmodel.StationViewModel] 各フラグメントで共有するためのViewModelStoreを提供する
 * @author Seo-4d696b75
 * @version 2020/12/22.
 */
open class AppFragment : Fragment(), ViewModelStoreOwner {

    private val _service by inject<ServiceGetter>()
    private val _mainViewModelStore by inject<ViewModelStore>()

    @MainThread
    fun getService(block: (StationService) -> Unit) {
        _service.get(block)
    }

    override fun getViewModelStore(): ViewModelStore {
        return _mainViewModelStore
    }
}

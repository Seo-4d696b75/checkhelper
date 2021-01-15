package jp.seo.station.ekisagasu.ui

import androidx.annotation.MainThread
import androidx.fragment.app.Fragment
import dagger.hilt.android.AndroidEntryPoint
import jp.seo.station.ekisagasu.core.StationService
import jp.seo.station.ekisagasu.utils.ServiceGetter
import javax.inject.Inject

/**
 * アプリのUI実装に必要な共通機能を提供する基底クラス
 *
 * - [StationService] へアクセスするための機能
 * @author Seo-4d696b75
 * @version 2020/12/22.
 */
@AndroidEntryPoint
open class AppFragment : Fragment() {

    @Inject
    lateinit var service: ServiceGetter


    @MainThread
    fun getService(block: (StationService) -> Unit) {
        service.get(block)
    }

}

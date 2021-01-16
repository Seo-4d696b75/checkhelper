package jp.seo.station.ekisagasu.ui

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelStore
import dagger.hilt.android.AndroidEntryPoint
import jp.seo.station.ekisagasu.core.StationRepository
import jp.seo.station.ekisagasu.core.StationService
import jp.seo.station.ekisagasu.core.UserRepository
import jp.seo.station.ekisagasu.viewmodel.ApplicationViewModel
import jp.seo.station.ekisagasu.viewmodel.MainViewModel
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
    lateinit var singletonStore: ViewModelStore

    @Inject
    lateinit var stationRepository: StationRepository

    @Inject
    lateinit var userRepository: UserRepository

    private val appViewModel: ApplicationViewModel by lazy {
        ApplicationViewModel.getInstance { singletonStore }
    }

    val mainViewModel: MainViewModel by lazy {
        MainViewModel.getInstance(
            requireActivity(),
            appViewModel,
            stationRepository,
            userRepository
        )
    }
}

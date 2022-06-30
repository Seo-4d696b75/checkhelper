package jp.seo.station.ekisagasu.ui

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelStore
import dagger.hilt.android.AndroidEntryPoint
import jp.seo.station.ekisagasu.core.NavigationRepository
import jp.seo.station.ekisagasu.core.StationRepository
import jp.seo.station.ekisagasu.core.UserRepository
import jp.seo.station.ekisagasu.repository.LocationRepository
import jp.seo.station.ekisagasu.viewmodel.ApplicationViewModel
import jp.seo.station.ekisagasu.viewmodel.MainViewModel
import javax.inject.Inject

/**
 * アプリのUI実装に必要な共通機能を提供する基底クラス
 *
 * @author Seo-4d696b75
 * @version 2020/12/22.
 */
@AndroidEntryPoint
open class AppFragment : Fragment() {

    /**
     * Applicationレベルで共有（Singletonスコープ）したいViewModelの保持に利用する
     */
    @Inject
    lateinit var singletonStore: ViewModelStore

    @Inject
    lateinit var stationRepository: StationRepository

    @Inject
    lateinit var userRepository: UserRepository

    @Inject
    lateinit var locationRepository: LocationRepository

    @Inject
    lateinit var navigator: NavigationRepository

    /**
     * Applicationレベル(Singletonスコープ)で共有するViewModel
     *
     * 特にServiceとの情報共有はこのインスタンス経由が必須
     */
    val appViewModel: ApplicationViewModel by lazy {
        ApplicationViewModel.getInstance(
            { singletonStore },
            stationRepository,
            userRepository,
            locationRepository,
            navigator
        )
    }

    /**
     * Activityレベル(Activityスコープ）で共有するViewModel
     *
     * [MainActivity]とそれに載っている[AppFragment]で共有される
     */
    val mainViewModel: MainViewModel by lazy {
        MainViewModel.getInstance(
            requireActivity(),
            stationRepository,
            userRepository
        )
    }
}

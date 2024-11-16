package com.seo4d696b75.android.ekisagasu.ui.service

import androidx.lifecycle.HasDefaultViewModelProviderFactory
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelLazy
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.seo4d696b75.android.ekisagasu.domain.dataset.PrefectureRepository
import com.seo4d696b75.android.ekisagasu.domain.location.LocationRepository
import com.seo4d696b75.android.ekisagasu.domain.navigator.NavigatorRepository
import com.seo4d696b75.android.ekisagasu.domain.screen.ScreenRepository
import com.seo4d696b75.android.ekisagasu.domain.search.StationSearchRepository
import com.seo4d696b75.android.ekisagasu.domain.user.UserSettingRepository
import com.seo4d696b75.android.ekisagasu.ui.popup.PopupViewModel
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
annotation class ServiceViewModel

// ViewModelの取得には ViewModelStore, ViewModelProvider.Factory が必要
interface ServiceViewModelComponent : ViewModelStoreOwner, HasDefaultViewModelProviderFactory

/**
 * ServiceでViewModelを取得するためのイディオム `val viewModel: VM by viewModels()`
 */
inline fun <reified VM : ViewModel> ServiceViewModelComponent.viewModels(): Lazy<VM> = ViewModelLazy(
    viewModelClass = VM::class,
    storeProducer = { viewModelStore },
    factoryProducer = { defaultViewModelProviderFactory },
    extrasProducer = { defaultViewModelCreationExtras },
)

// service では HiltViewModel に対応した factory 不在のため独自に用意する
@Suppress("unused")
@Module
@InstallIn(SingletonComponent::class)
object ViewModelModule {
    @ServiceViewModel
    @Provides
    @Singleton
    fun provideViewModelStore() = ViewModelStore()

    @Provides
    fun provideServiceViewModelComponent(
        @ServiceViewModel viewModelStore: ViewModelStore,
        @ServiceViewModel viewModelFactory: ViewModelProvider.Factory,
    ) = object : ServiceViewModelComponent {
        override val viewModelStore = viewModelStore
        override val defaultViewModelProviderFactory = viewModelFactory
    }

    @ServiceViewModel
    @Provides
    @Singleton
    fun provideViewModelFactory(
        settingRepository: UserSettingRepository,
        searchRepository: StationSearchRepository,
        prefectureRepository: PrefectureRepository,
        navigatorRepository: NavigatorRepository,
        locationRepository: LocationRepository,
        screenRepository: ScreenRepository,
    ) = viewModelFactory {
        // 各ViewModelごとに指定すること
        initializer {
            PopupViewModel(
                settingRepository,
                searchRepository,
                prefectureRepository,
                navigatorRepository,
                locationRepository,
                screenRepository,
            )
        }
    }
}

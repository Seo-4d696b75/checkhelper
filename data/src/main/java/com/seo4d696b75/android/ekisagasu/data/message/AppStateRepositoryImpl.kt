package com.seo4d696b75.android.ekisagasu.data.message

import com.seo4d696b75.android.ekisagasu.domain.coroutine.ExternalScope
import com.seo4d696b75.android.ekisagasu.domain.message.AppStateRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

class AppStateRepositoryImpl @Inject constructor(
    @ExternalScope private val scope: CoroutineScope,
) : AppStateRepository {
    override var isServiceRunning = false
    override var hasPermissionChecked = false
    override var hasDataVersionChecked = false

    private val _nightMode = MutableStateFlow(false)

    override val nightMode = _nightMode.asStateFlow()

    override fun setNightMode(enabled: Boolean) {
        _nightMode.update { enabled }
    }

    private val _finishApp = MutableSharedFlow<Unit>()

    override val appFinish = _finishApp.asSharedFlow()

    override suspend fun requestAppFinish() {
        _finishApp.emit(Unit)
    }
}

@Suppress("unused")
@Module
@InstallIn(SingletonComponent::class)
interface AppStateRepositoryModule {
    @Binds
    @Singleton
    fun bindAppStateRepository(impl: AppStateRepositoryImpl): AppStateRepository
}

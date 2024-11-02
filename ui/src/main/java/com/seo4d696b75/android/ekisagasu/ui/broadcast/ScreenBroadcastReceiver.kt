package com.seo4d696b75.android.ekisagasu.ui.broadcast

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.seo4d696b75.android.ekisagasu.domain.screen.ScreenRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScreenBroadcastReceiver @Inject constructor() : BroadcastReceiver(), ScreenRepository {

    private val _isTurnOn = MutableStateFlow(true)
    override val isTurnOn = _isTurnOn.asStateFlow()

    override fun onReceive(context: Context?, intent: Intent?) {
        intent?.action?.let {
            when (it) {
                Intent.ACTION_SCREEN_OFF -> {
                    _isTurnOn.update { false }
                }

                Intent.ACTION_SCREEN_ON,
                Intent.ACTION_USER_PRESENT -> {
                    _isTurnOn.update { true }
                }

                else -> {}
            }
        }
    }
}

@Suppress("unused")
@Module
@InstallIn(SingletonComponent::class)
interface ScreenRepositoryModule {
    @Binds
    fun bind(impl: ScreenBroadcastReceiver): ScreenRepository
}

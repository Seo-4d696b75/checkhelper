package com.seo4d696b75.android.ekisagasu.ui.broadcast

import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import com.seo4d696b75.android.ekisagasu.domain.screen.ScreenRepository
import com.seo4d696b75.android.ekisagasu.domain.screen.ScreenStatus
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScreenBroadcastReceiver @Inject constructor(
    @ApplicationContext context: Context,
) : BroadcastReceiver(), ScreenRepository {
    private val keyguardManager = context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager

    private val statusFlow = MutableStateFlow<ScreenStatus>(ScreenStatus.TurnOn(false))
    override val status = statusFlow.asStateFlow()

    override val isScreenLocked: Boolean
        get() = keyguardManager.isKeyguardLocked

    override fun onReceive(context: Context?, intent: Intent?) {
        intent?.action?.let {
            when (it) {
                Intent.ACTION_SCREEN_OFF -> {
                    statusFlow.update { ScreenStatus.TurnOff }
                }

                Intent.ACTION_SCREEN_ON -> {
                    statusFlow.update { ScreenStatus.TurnOn(isScreenLocked) }
                }

                Intent.ACTION_USER_PRESENT -> {
                    statusFlow.update { ScreenStatus.TurnOn(false) }
                }

                else -> {}
            }
        }
    }

    override fun invalidate() {
        statusFlow.update {
            if (powerManager.isInteractive) {
                ScreenStatus.TurnOn(isScreenLocked)
            } else {
                ScreenStatus.TurnOff
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

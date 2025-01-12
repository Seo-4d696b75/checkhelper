package com.seo4d696b75.android.ekisagasu.ui.update

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import com.seo4d696b75.android.ekisagasu.domain.dataset.LatestDataVersion
import com.seo4d696b75.android.ekisagasu.domain.dataset.update.DataUpdateType
import com.seo4d696b75.android.ekisagasu.ui.event.NavigationEvent
import com.seo4d696b75.android.ekisagasu.ui.event.NavigationEventHolder
import com.seo4d696b75.android.ekisagasu.ui.event.navigationEventHolder
import com.seo4d696b75.android.ekisagasu.ui.navigation.NavigationRoute
import dagger.Module
import dagger.Provides
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import javax.inject.Singleton

sealed interface DataUpdateNavigationEvent : NavigationEvent {

    data class ConfirmUpdate(val type: DataUpdateType, val info: LatestDataVersion) : DataUpdateNavigationEvent

    data class RequestUpdate(val type: DataUpdateType, val info: LatestDataVersion) : DataUpdateNavigationEvent

    data class UpdateSuccess(val info: LatestDataVersion) : DataUpdateNavigationEvent
}

@Suppress("unused")
@Module
@InstallIn(SingletonComponent::class)
object DataUpdateNavigationEventHolderModule {
    @Provides
    @Singleton
    fun provideEventHolder(): NavigationEventHolder<DataUpdateNavigationEvent> = navigationEventHolder()
}

class NavigateDataUpdateEvent @Inject constructor(
    private val holder: NavigationEventHolder<DataUpdateNavigationEvent>,
) {
    operator fun invoke(nav: DataUpdateNavigationEvent) {
        holder.navigate(nav)
    }
}

@Composable
fun DataUpdateNavigationEvent(controller: NavController) {
    val context = LocalContext.current
    val holder = remember(context) {
        val entryPoint =
            EntryPointAccessors.fromApplication(context, DataUpdateNavigationEventHolderEntryPoint::class.java)
        entryPoint.holder
    }
    NavigationEvent(holder) {
        val route = when (it) {
            is DataUpdateNavigationEvent.ConfirmUpdate ->
                NavigationRoute.DataUpdate.ConfirmUpdateDialog(it.type, it.info)

            is DataUpdateNavigationEvent.RequestUpdate -> NavigationRoute.DataUpdate.UpdateDialog(it.type, it.info)
            is DataUpdateNavigationEvent.UpdateSuccess -> NavigationRoute.DataUpdate.UpdateSuccessDialog(it.info)
        }
        controller.navigate(route)
    }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
private interface DataUpdateNavigationEventHolderEntryPoint {
    val holder: NavigationEventHolder<DataUpdateNavigationEvent>
}

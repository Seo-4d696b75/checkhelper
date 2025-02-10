package com.seo4d696b75.android.ekisagasu.ui.selectLine

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
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

data class SelectLineNavigationEvent(val type: LineSelectType) : NavigationEvent

@Suppress("unused")
@Module
@InstallIn(SingletonComponent::class)
object SelectLineNavigationEventHolderModule {
    @Provides
    @Singleton
    fun provideHolder(): NavigationEventHolder<SelectLineNavigationEvent> = navigationEventHolder()
}

class NavigateSelectLineEvent @Inject constructor(
    private val holder: NavigationEventHolder<SelectLineNavigationEvent>
) {
    operator fun invoke(type: LineSelectType) {
        val event = SelectLineNavigationEvent(type)
        holder.navigate(event)
    }
}

@Composable
fun SelectLineNavigationEvent(controller: NavController) {
    val context = LocalContext.current
    val holder = remember(context) {
        val entryPoint =
            EntryPointAccessors.fromApplication(context, SelectLineNavigationEventHolderEntryPoint::class.java)
        entryPoint.selectLineEventHolder
    }
    NavigationEvent(holder) {
        val route = NavigationRoute.SelectLineDialog(it.type)
        controller.navigate(route)
    }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
private interface SelectLineNavigationEventHolderEntryPoint {
    val selectLineEventHolder: NavigationEventHolder<SelectLineNavigationEvent>
}

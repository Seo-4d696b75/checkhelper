package jp.seo.station.ekisagasu.hilt

import com.seo4d696b75.android.ekisagasu.domain.coroutine.ExternalScope
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import timber.log.Timber
import kotlin.coroutines.CoroutineContext

@Suppress("unused")
@InstallIn(SingletonComponent::class)
@Module
object ExternalScopeModule {

    @Provides
    @ExternalScope
    fun provideExternalScope(): CoroutineScope = object : CoroutineScope {
        private val job = SupervisorJob()

        private val coroutineExceptionHandler = CoroutineExceptionHandler { _, throwable ->
            Timber.e(throwable, "caught in external scope")
            throw throwable
        }

        override val coroutineContext: CoroutineContext
            get() = job + Dispatchers.Main.immediate + coroutineExceptionHandler
    }
}

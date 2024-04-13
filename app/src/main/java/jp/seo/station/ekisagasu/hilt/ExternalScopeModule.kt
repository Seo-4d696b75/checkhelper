package jp.seo.station.ekisagasu.hilt

import com.seo4d696b75.android.ekisagasu.domain.coroutine.ExternalScope
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import timber.log.Timber
import javax.inject.Singleton
import kotlin.coroutines.CoroutineContext

@Suppress("unused")
@InstallIn(SingletonComponent::class)
@Module
object ExternalScopeModule {

    @Singleton
    @Provides
    @ExternalScope
    fun provideExternalScopeJob(): Job = SupervisorJob()

    @Singleton
    @Provides
    @ExternalScope
    fun provideExternalScope(
        @ExternalScope parent: Job,
    ): CoroutineScope = object : CoroutineScope {
        private val job = SupervisorJob(parent)

        private val coroutineExceptionHandler = CoroutineExceptionHandler { _, throwable ->
            Timber.e(throwable, "caught in external scope")
        }

        override val coroutineContext: CoroutineContext
            get() = job + Dispatchers.Main.immediate + coroutineExceptionHandler
    }
}

package com.seo4d696b75.android.ekisagasu.data.error

import com.seo4d696b75.android.ekisagasu.domain.error.ErrorHolder
import com.seo4d696b75.android.ekisagasu.domain.error.ErrorState
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
class ErrorHolderImpl @Inject constructor() : ErrorHolder {

    private val _state = MutableStateFlow<ErrorState>(ErrorState.Empty)
    override val state = _state.asStateFlow()

    override fun enqueue(error: Throwable) {
        _state.update { ErrorState.Queued(error) }
    }

    override fun consume() {
        _state.update {
            require(it is ErrorState.Queued)
            it.copy(consumed = true)
        }
    }
}

@Suppress("unused")
@Module
@InstallIn(SingletonComponent::class)
interface ErrorHolderModule {
    @Binds
    fun bind(impl: ErrorHolderImpl): ErrorHolder
}

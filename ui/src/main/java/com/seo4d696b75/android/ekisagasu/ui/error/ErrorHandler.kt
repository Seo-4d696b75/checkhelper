package com.seo4d696b75.android.ekisagasu.ui.error

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.seo4d696b75.android.ekisagasu.ui.R
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityRetainedComponent
import kotlinx.coroutines.CancellationException
import timber.log.Timber
import javax.inject.Inject

interface ErrorHandler {
    /**
     * 例外を補足したらエラー表示する
     *
     * @param configuration 例外を補足したタイミングまで評価は遅延される
     */
    fun <T> Result<T>.messageOnError(configuration: ErrorMessageConfigureScope.(error: Throwable) -> Unit): Result<T>
}

/**
 * エラー表示のUIコンテンツを指定する
 */
interface ErrorMessageConfigureScope {
    var title: (@Composable () -> String)?
    var description: (@Composable () -> String)?
    var onClosed: (() -> Unit)?
}

class ErrorHandlerImpl @Inject constructor(
    private val holder: ErrorStateHolder,
) : ErrorHandler {
    override fun <T> Result<T>.messageOnError(
        configuration: ErrorMessageConfigureScope.(error: Throwable) -> Unit,
    ): Result<T> =
        onFailure {
            if (it is CancellationException) {
                throw it
            } else {
                Timber.w(it, "caught in Result.messageOnError")
                val configureScope = object : ErrorMessageConfigureScope {
                    override var title: (@Composable () -> String)? = null
                    override var description: (@Composable () -> String)? = null
                    override var onClosed: (() -> Unit)? = null
                }
                with(configureScope) { configuration(it) }
                val message = ErrorUiMessage(
                    title = configureScope.title ?: {
                        stringResource(R.string.dialog_error_title_default)
                    },
                    description = configureScope.description ?: {
                        stringResource(id = R.string.dialog_error_description_default, it.message ?: "")
                    },
                    onClosed = configureScope.onClosed,
                )
                holder.enqueue(it, message)
            }
        }
}

@Suppress("unused")
@Module
@InstallIn(ActivityRetainedComponent::class)
interface ErrorHandlerModule {
    @Binds
    fun bind(impl: ErrorHandlerImpl): ErrorHandler
}

package jp.seo.station.ekisagasu.log

import jp.seo.station.ekisagasu.model.AppMessage
import jp.seo.station.ekisagasu.repository.AppStateRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * データベースに保存する必要のあるログのみ流す
 */
open class ReleaseLogTree(
    defaultDispatcher: CoroutineDispatcher,
    private val appStateRepository: AppStateRepository,
) : LogTree(), CoroutineScope {
    override fun onDebugMessage(
        tag: String?,
        message: String,
    ) {
        // no output
    }

    override fun onLogMessage(
        tag: String?,
        message: String,
    ) {
        launch {
            appStateRepository.emitMessage(
                AppMessage.Log(message),
            )
        }
    }

    override fun onErrorMessage(
        tag: String?,
        message: String,
    ) {
        launch {
            appStateRepository.emitMessage(
                AppMessage.Error(message),
            )
        }
    }

    private val job = Job()

    override val coroutineContext = defaultDispatcher + job
}

package jp.seo.station.ekisagasu.log

import android.annotation.SuppressLint
import android.util.Log
import jp.seo.station.ekisagasu.repository.AppStateRepository
import kotlinx.coroutines.CoroutineDispatcher

/**
 * Logcatにも出力する
 */
@SuppressLint("LogNotTimber")
class DebugLogTree(
    defaultDispatcher: CoroutineDispatcher,
    appStateRepository: AppStateRepository,
) : ReleaseLogTree(
    defaultDispatcher, appStateRepository
) {
    override fun onDebugMessage(tag: String?, message: String) {
        Log.d(tag, message)
        super.onDebugMessage(tag, message)
    }

    override fun onLogMessage(tag: String?, message: String) {
        Log.i(tag, message)
        super.onLogMessage(tag, message)
    }

    override fun onErrorMessage(tag: String?, message: String) {
        Log.e(tag, message)
        super.onErrorMessage(tag, message)
    }
}

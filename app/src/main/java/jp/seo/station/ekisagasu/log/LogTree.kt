package jp.seo.station.ekisagasu.log

import android.util.Log
import timber.log.Timber
import java.io.PrintWriter
import java.io.StringWriter

open class LogTree : Timber.Tree() {
    final override fun log(
        priority: Int,
        tag: String?,
        message: String,
        t: Throwable?,
    ) {
        val str = formatMessage(message, t)
        when (priority) {
            Log.VERBOSE, Log.DEBUG -> {
                onDebugMessage(tag, str)
            }
            Log.INFO, Log.WARN -> {
                onLogMessage(tag, str)
            }
            Log.ASSERT, Log.ERROR -> {
                onErrorMessage(tag, str)
            }
            else -> {
                onDebugMessage("LogTree", "detect unknown priority:$priority with log: $str")
            }
        }
    }

    private fun formatMessage(
        message: String,
        t: Throwable?,
    ) = if (t != null) {
        val sw = StringWriter()
        val pw = PrintWriter(sw)
        t.printStackTrace(pw)
        String.format("%s caused by;\n%s", message, sw.toString())
    } else {
        message
    }

    /**
     * 開発者向け専用のメッセージ
     *
     * Debugモードでのみの出力を想定し、Appのデータベースには保存しないぐらいのレベル
     */
    open fun onDebugMessage(
        tag: String?,
        message: String,
    ) {}

    /**
     * 開発者＆ユーザ向けのメッセージ
     *
     * Releaseモードでも利用する想定.
     * Appのデータベースに保存して永続化する必要のあるレベル
     */
    open fun onLogMessage(
        tag: String?,
        message: String,
    ) {}

    /**
     * アプリの正常な実行が困難なレベルのメッセージ
     */
    open fun onErrorMessage(
        tag: String?,
        message: String,
    ) {}
}

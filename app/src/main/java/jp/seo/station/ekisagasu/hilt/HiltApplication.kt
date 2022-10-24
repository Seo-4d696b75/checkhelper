package jp.seo.station.ekisagasu.hilt

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import jp.seo.station.ekisagasu.model.AppMessage
import jp.seo.station.ekisagasu.repository.LogRepository
import jp.seo.station.ekisagasu.usecase.AppFinishUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

/**
 * このApplication全般の依存をinjectしたcontext
 *
 * アーキテクチャの構築方針  [hint][https://stackoverflow.com/questions/53382320/boundservice-livedata-viewmodel-best-practice-in-new-android-recommended-arc]
 * - Activity, Fragment と同様にServiceもデータとは別のライフサイクルを持つAndroidコンポーネントとして同列扱い
 * - Serviceはライフサイクルを扱えるように`LifecycleService` を利用
 * - データの取得はすべてViewModel経由で行う
 * - ViewModelの取得に必要な依存関係はHiltで自動注入する
 * - ViewModel以下のアーキテクチャはAndroid推奨の通り
 *
 * @author Seo-4d696b75
 * @version 2021/01/13.
 */
@HiltAndroidApp
class HiltApplication : Application() {

    @Inject
    lateinit var logRepository: LogRepository

    @Inject
    lateinit var appFinishUseCase: AppFinishUseCase

    override fun onCreate() {
        super.onCreate()
        var crashStarting = false
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { _, e ->
            if (crashStarting) return@setDefaultUncaughtExceptionHandler
            crashStarting = true
            try {
                runBlocking(Dispatchers.Default) {
                    // ログ出力
                    logRepository.saveMessage(
                        AppMessage.Error("UnhandledException", e)
                    )
                    // 終了処理
                    appFinishUseCase()
                }
            } finally {
                Thread.setDefaultUncaughtExceptionHandler(defaultHandler)
            }
        }
    }
}

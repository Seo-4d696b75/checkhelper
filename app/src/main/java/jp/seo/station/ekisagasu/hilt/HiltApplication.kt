package jp.seo.station.ekisagasu.hilt

import android.app.Application
import com.seo4d696b75.android.ekisagasu.domain.lifecycle.AppInitializer
import dagger.hilt.android.HiltAndroidApp
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
    lateinit var initializers: MutableSet<AppInitializer>

    override fun onCreate() {
        super.onCreate()

        initializers.forEach {
            it.onCreate()
        }
    }
}

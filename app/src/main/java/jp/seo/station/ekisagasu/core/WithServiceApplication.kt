package jp.seo.station.ekisagasu.core

import android.app.Application
import androidx.lifecycle.ViewModelStore
import jp.seo.station.ekisagasu.utils.ServiceGetter
import org.koin.android.ext.android.startKoin
import org.koin.dsl.module.module

/**
 * Appの基幹機能を提供するserviceへのアクセスインターフェイスなど依存をinjectしたcontext
 * @author Seo-4d696b75
 * @version 2021/01/13.
 */
class WithServiceApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        startKoin(this, listOf(this.module))
    }

    private val module = module {
        single { ServiceGetter() }
        single { ViewModelStore() }
    }

}

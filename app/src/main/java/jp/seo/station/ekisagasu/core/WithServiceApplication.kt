package jp.seo.station.ekisagasu.core

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * このApplication全般の依存をinjectしたcontext
 * @author Seo-4d696b75
 * @version 2021/01/13.
 */
@HiltAndroidApp
class WithServiceApplication : Application()

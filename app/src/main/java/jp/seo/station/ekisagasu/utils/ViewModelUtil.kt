package jp.seo.station.ekisagasu.utils

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

/**
 * @author Seo-4d696b75
 * @version 2020/12/16.
 */

fun <M : ViewModel> getViewModelFactory(constructor: () -> M): ViewModelProvider.Factory {
    return object : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("unchecked_cast")
            return constructor() as T
        }
    }
}

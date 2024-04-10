package com.seo4d696b75.android.ekisagasu.domain.dataset.update

sealed interface DataUpdateProgress {
    data class Download(val percent: Int) : DataUpdateProgress

    data object Save : DataUpdateProgress
}

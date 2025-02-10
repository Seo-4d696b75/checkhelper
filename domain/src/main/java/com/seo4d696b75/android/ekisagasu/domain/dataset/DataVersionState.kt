package com.seo4d696b75.android.ekisagasu.domain.dataset

sealed interface DataVersionState {
    data object None : DataVersionState

    data class Initialized(val version: DataVersion) : DataVersionState
}

package com.seo4d696b75.android.ekisagasu.domain.error

import kotlinx.coroutines.flow.Flow

interface ErrorHolder {
    val state: Flow<ErrorState>
    fun enqueue(error: Throwable)
    fun consume()
}

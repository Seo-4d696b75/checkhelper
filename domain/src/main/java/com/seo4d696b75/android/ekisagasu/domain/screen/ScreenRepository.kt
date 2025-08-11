package com.seo4d696b75.android.ekisagasu.domain.screen

import kotlinx.coroutines.flow.Flow

interface ScreenRepository {
    val status: Flow<ScreenStatus>
    val isScreenLocked: Boolean
    fun invalidate()
}

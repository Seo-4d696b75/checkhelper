package com.seo4d696b75.android.ekisagasu.domain.screen

import kotlinx.coroutines.flow.Flow

interface PopupStatusRepository {
    val status: Flow<PopupStatus>
    fun closePopup()
    fun togglePopup()
}

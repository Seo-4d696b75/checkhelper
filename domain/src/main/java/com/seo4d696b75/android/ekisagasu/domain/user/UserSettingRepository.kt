package com.seo4d696b75.android.ekisagasu.domain.user

import kotlinx.coroutines.flow.StateFlow

/**
 * @author Seo-4d696b75
 * @version 2020/12/16.
 */
interface UserSettingRepository {
    val setting: StateFlow<UserSetting>

    fun update(producer: (UserSetting) -> UserSetting)

    suspend fun load()

    suspend fun save()
}

package com.seo4d696b75.android.ekisagasu.domain.user

import kotlinx.coroutines.flow.Flow

/**
 * @author Seo-4d696b75
 * @version 2020/12/16.
 */
interface UserSettingRepository {
    val setting: Flow<UserSetting>
    suspend fun update(producer: (UserSetting) -> UserSetting)
}

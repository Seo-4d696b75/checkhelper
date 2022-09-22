package jp.seo.station.ekisagasu.repository

import jp.seo.station.ekisagasu.model.UserSetting
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow

/**
 * @author Seo-4d696b75
 * @version 2020/12/16.
 */
interface UserSettingRepository {
    val setting: Flow<UserSetting>
    fun update(coroutineScope: CoroutineScope, producer: (UserSetting) -> UserSetting)
}

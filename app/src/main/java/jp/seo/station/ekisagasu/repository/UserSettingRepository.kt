package jp.seo.station.ekisagasu.repository

import jp.seo.station.ekisagasu.model.UserSetting
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * @author Seo-4d696b75
 * @version 2020/12/16.
 */
interface UserSettingRepository {
    val setting: MutableStateFlow<UserSetting>
    suspend fun load()
    suspend fun save()
}
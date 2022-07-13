package jp.seo.station.ekisagasu.usecase

import jp.seo.station.ekisagasu.core.DataVersion

sealed interface DataUpdateResult {
    object Failure : DataUpdateResult
    data class Success(val version: DataVersion) : DataUpdateResult
}
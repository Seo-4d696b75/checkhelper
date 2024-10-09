package com.seo4d696b75.android.ekisagasu.ui.log

import android.os.Parcel
import android.os.Parcelable
import kotlinx.parcelize.Parceler
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.TypeParceler
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Parcelize
@TypeParceler<LogOutputConfig, LogOutputConfigParceler>
data class LogOutputConfigArg(
    val value: LogOutputConfig,
) : Parcelable

class LogOutputConfigParceler : Parceler<LogOutputConfig> {
    override fun create(parcel: Parcel): LogOutputConfig {
        val str = requireNotNull(parcel.readString())
        return Json.decodeFromString(str)
    }

    override fun LogOutputConfig.write(parcel: Parcel, flags: Int) {
        val str = Json.encodeToString(this)
        parcel.writeString(str)
    }
}

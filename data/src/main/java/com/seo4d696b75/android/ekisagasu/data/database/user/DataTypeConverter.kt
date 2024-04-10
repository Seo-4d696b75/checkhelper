package com.seo4d696b75.android.ekisagasu.data.database.user

import androidx.room.TypeConverter
import com.seo4d696b75.android.ekisagasu.domain.log.AppLogType
import java.util.Date

class TimestampConverter {
    @TypeConverter
    fun convertDatetime(value: Date?): Long? = value?.time

    @TypeConverter
    fun convertUnixTime(value: Long?): Date? = value?.let {
        Date(it)
    }
}

class AppLogTypeConverter {
    @TypeConverter
    fun fromAppLogType(type: AppLogType?): Int? = type?.value

    @TypeConverter
    fun toAppLogType(value: Int?): AppLogType? = AppLogType.entries.firstOrNull { it.value == value }
}

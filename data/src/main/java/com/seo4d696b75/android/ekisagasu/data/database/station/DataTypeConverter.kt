package com.seo4d696b75.android.ekisagasu.data.database.station

import androidx.room.TypeConverter
import com.seo4d696b75.android.ekisagasu.domain.kdtree.StationNode
import com.seo4d696b75.android.ekisagasu.domain.dataset.StationRegistration
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.json.JSONObject

/**
 * @author Seo-4d696b75
 * @version 2020/12/18.
 */

class NodeListConverter {
    @TypeConverter
    fun convertRegistration(value: List<StationNode>?): String? = value?.let {
        Json.encodeToString(it)
    }

    @TypeConverter
    fun convertJson(value: String?): List<StationNode>? = value?.let {
        Json.decodeFromString<List<StationNode>>(it)
    }
}

class RegistrationListConverter {
    @TypeConverter
    fun convertArray(value: Array<StationRegistration>?): String? = value?.let {
        Json.encodeToString(it)
    }

    @TypeConverter
    fun convertJson(value: String?): Array<StationRegistration>? = value?.let {
        Json.decodeFromString<Array<StationRegistration>>(it)
    }
}

class ArrayIntConverter {
    @TypeConverter
    fun convertArray(value: Array<Int>?): String? = value?.let {
        Json.encodeToString(it)
    }

    @TypeConverter
    fun convertJson(value: String?): Array<Int>? = value?.let {
        Json.decodeFromString<Array<Int>>(it)
    }
}

class IntListConverter {
    @TypeConverter
    fun convertList(value: List<Int>?): String? = value?.let {
        Json.encodeToString(it)
    }

    @TypeConverter
    fun convertJson(value: String?): List<Int>? = value?.let {
        Json.decodeFromString<List<Int>>(it)
    }
}

class JSONRecordConverter {
    @TypeConverter
    fun convertObject(value: JSONObject?): String? = value?.let {
        value.toString()
    }

    @TypeConverter
    fun convertJson(value: String?): JSONObject? = value?.let {
        JSONObject(value)
    }
}

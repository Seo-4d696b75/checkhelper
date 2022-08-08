package jp.seo.station.ekisagasu.database

import androidx.room.TypeConverter
import jp.seo.station.ekisagasu.model.StationNode
import jp.seo.station.ekisagasu.model.StationRegistration
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
    fun convertRegistration(value: List<StationNode>?): String? {
        return value?.let {
            Json.encodeToString(it)
        }
    }

    @TypeConverter
    fun convertJson(value: String?): List<StationNode>? {
        return value?.let {
            Json.decodeFromString<List<StationNode>>(it)
        }
    }
}

class RegistrationListConverter {
    @TypeConverter
    fun convertArray(value: Array<StationRegistration>?): String? {
        return value?.let {
            Json.encodeToString(it)
        }
    }

    @TypeConverter
    fun convertJson(value: String?): Array<StationRegistration>? {
        return value?.let {
            Json.decodeFromString<Array<StationRegistration>>(it)
        }
    }
}

class ArrayIntConverter {

    @TypeConverter
    fun convertArray(value: Array<Int>?): String? {
        return value?.let {
            Json.encodeToString(it)
        }
    }

    @TypeConverter
    fun convertJson(value: String?): Array<Int>? {
        return value?.let {
            Json.decodeFromString<Array<Int>>(it)
        }
    }
}

class IntListConverter {
    @TypeConverter
    fun convertList(value: List<Int>?): String? {
        return value?.let {
            Json.encodeToString(it)
        }
    }

    @TypeConverter
    fun convertJson(value: String?): List<Int>? {
        return value?.let {
            Json.decodeFromString<List<Int>>(it)
        }
    }
}

class JSONRecordConverter {
    @TypeConverter
    fun convertObject(value: JSONObject?): String? {
        return value?.let {
            value.toString()
        }
    }

    @TypeConverter
    fun convertJson(value: String?): JSONObject? {
        return value?.let {
            JSONObject(value)
        }
    }
}

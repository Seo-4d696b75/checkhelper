package jp.seo.station.ekisagasu.utils

import androidx.room.TypeConverter
import com.google.android.gms.maps.model.LatLng
import com.google.gson.*
import jp.seo.station.ekisagasu.StationRegistration
import jp.seo.station.ekisagasu.search.StationNode
import org.json.JSONObject
import java.lang.reflect.Type

/**
 * @author Seo-4d696b75
 * @version 2020/12/18.
 */


class PositionJsonConverter : JsonSerializer<LatLng>, JsonDeserializer<LatLng> {

    override fun serialize(
        src: LatLng?,
        typeOfSrc: Type?,
        context: JsonSerializationContext?
    ): JsonElement {
        val obj = JsonObject()
        return src?.let {
            obj.addProperty("lat", it.latitude)
            obj.addProperty("lng", it.longitude)
            return obj
        } ?: JsonNull.INSTANCE
    }

    override fun deserialize(
        json: JsonElement?,
        typeOfT: Type?,
        context: JsonDeserializationContext?
    ): LatLng {
        if (json == JsonNull.INSTANCE || json == null) {
            return LatLng(0.0, 0.0)
        }
        val obj = json.asJsonObject
        val lat = obj["lat"].asDouble
        val lng = obj["lng"].asDouble
        return LatLng(lat, lng)
    }

}

class PositionRecordConverter {

    @TypeConverter
    fun convertPos(value: LatLng?): String? {
        return value?.let {
            GsonBuilder()
                .registerTypeAdapter(LatLng::class.java, PositionJsonConverter())
                .serializeNulls()
                .create()
                .toJson(value)
        }

    }

    @TypeConverter
    fun convertJson(value: String?): LatLng? {
        return value?.let {
            GsonBuilder()
                .registerTypeAdapter(LatLng::class.java, PositionJsonConverter())
                .serializeNulls()
                .create()
                .fromJson(value, LatLng::class.java)
        }
    }
}

class RegistrationConverter {

    @TypeConverter
    fun convertRegistration(value: StationRegistration?): String? {
        return value?.let {
            GsonBuilder()
                .serializeNulls()
                .create()
                .toJson(value)
        }

    }

    @TypeConverter
    fun convertJson(value: String?): StationRegistration? {
        return value?.let {
            GsonBuilder()
                .serializeNulls()
                .create()
                .fromJson(value, StationRegistration::class.java)
        }
    }
}

class NodeListConverter {

    @TypeConverter
    fun convertRegistration(value: List<StationNode>?): String? {
        return value?.let {
            GsonBuilder()
                .serializeNulls()
                .create()
                .toJson(value)
        }

    }

    @TypeConverter
    fun convertJson(value: String?): List<StationNode>? {
        return value?.let {
            GsonBuilder()
                .serializeNulls()
                .create()
                .fromJson(value, Array<StationNode>::class.java)
                .toList()
        }
    }
}

class RegistrationListConverter{
    @TypeConverter
    fun convertArray(value: Array<StationRegistration>?): String? {
        return value?.let {
            GsonBuilder()
                .serializeNulls()
                .create()
                .toJson(value)
        }

    }

    @TypeConverter
    fun convertJson(value: String?): Array<StationRegistration>? {
        return value?.let {
            GsonBuilder()
                .serializeNulls()
                .create()
                .fromJson(value, Array<StationRegistration>::class.java)
        }
    }
}

class ArrayIntConverter {

    @TypeConverter
    fun convertArray(value: Array<Int>?): String? {
        return value?.let {
            Gson().toJson(value)
        }

    }

    @TypeConverter
    fun convertJson(value: String?): Array<Int>? {
        return value?.let {
            Gson().fromJson(value, Array<Int>::class.java)
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

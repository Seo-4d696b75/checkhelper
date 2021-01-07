package jp.seo.station.ekisagasu

import androidx.room.*
import com.google.gson.*
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import java.lang.reflect.Type

/**
 * @author Seo-4d696b75
 * @version 2020/12/17.
 */
@Entity(tableName = "station", indices = [Index(value = ["id", "code"], unique = true)])
data class Station constructor(
    @ColumnInfo(name = "id")
    @Expose
    val id: String,
    @PrimaryKey
    @ColumnInfo(name = "code")
    @Expose
    val code: Int,
    @ColumnInfo(name = "lat")
    @Expose
    val lat: Double,
    @ColumnInfo(name = "lng")
    @Expose
    val lng: Double,
    @ColumnInfo(name = "name")
    @Expose
    val name: String,
    @ColumnInfo(name = "original_name")
    val originalName: String,
    @ColumnInfo(name = "name_kana")
    @SerializedName("name_kana") @Expose
    val nameKana: String,
    @ColumnInfo(name = "prefecture")
    @Expose
    val prefecture: Int,
    @ColumnInfo(name = "lines")
    @Expose
    val lines: Array<Int>,
    @ColumnInfo(name = "next")
    @Expose
    val next: Array<Int>,
    @ColumnInfo(name = "voronoi")
    @Expose(deserialize = false)
    val voronoi: String,

    ) {


    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Station

        if (id != other.id) return false
        if (code != other.code) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + code
        return result
    }

}

class StationConverter : JsonDeserializer<Station> {
    override fun deserialize(
        json: JsonElement?,
        typeOfT: Type?,
        context: JsonDeserializationContext?
    ): Station {
        return json?.let {
            val obj = it.asJsonObject
            val gson = Gson()
            val name = obj["name"].asString
            Station(
                obj["id"].asString,
                obj["code"].asInt,
                obj["lat"].asDouble,
                obj["lng"].asDouble,
                name,
                obj["original_name"]?.asString ?: name,
                obj["name_kana"]?.asString ?: "hoge",
                obj["prefecture"].asInt,
                gson.fromJson(obj["lines"], Array<Int>::class.java),
                gson.fromJson(obj["next"], Array<Int>::class.java),
                obj["voronoi"].toString()
            )
        } ?: Station("000000", 0, 0.0, 0.0, "none", "none", "none", 0, arrayOf(), arrayOf(), "")
    }

}

data class StationRegistration(
    @SerializedName("code")
    val code: Int,
    @SerializedName("numbering")
    val numbering: List<String>?
) {

    fun getNumberingString(): String {
        return numbering?.joinToString(separator = "/", transform = String::toString) ?: ""
    }

}

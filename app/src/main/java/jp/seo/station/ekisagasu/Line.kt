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
@Entity(tableName = "line", indices = [Index(value = ["id", "code"], unique = true)])
data class Line @JvmOverloads constructor(
    @ColumnInfo(name = "id")
    @SerializedName("id")
    @Expose
    val id: String,
    @PrimaryKey
    @ColumnInfo(name = "code")
    @SerializedName("code")
    @Expose
    val code: Int,
    @ColumnInfo(name = "name")
    @SerializedName("name")
    @Expose
    val name: String,
    @ColumnInfo(name = "name_kana")
    @SerializedName("name_kana")
    val nameKana: String,
    @ColumnInfo(name = "station_size")
    @SerializedName("station_size")
    @Expose
    val stationSize: Int,
    @ColumnInfo(name = "symbol")
    @SerializedName("symbol")
    @Expose
    val symbol: String?,
    @ColumnInfo(name = "color")
    @SerializedName("color")
    @Expose
    val color: String?,

    @ColumnInfo(name = "station_list")
    @SerializedName("station_list")
    @Expose
    val stationList: Array<StationRegistration>,
    @ColumnInfo(name = "polyline")
    @Expose(deserialize = false)
    val polyline: String?,

    ) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Line

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

class LineConverter : JsonDeserializer<Line> {
    override fun deserialize(
        json: JsonElement?,
        typeOfT: Type?,
        context: JsonDeserializationContext?
    ): Line {
        return json?.let {
            val obj = it.asJsonObject
            val gson = Gson()
            Line(
                obj["id"].asString,
                obj["code"].asInt,
                obj["name"].asString,
                obj["name_kana"].asString,
                obj["station_size"].asInt,
                obj["symbol"]?.asString,
                obj["color"]?.asString,
                gson.fromJson(obj["station_list"], Array<StationRegistration>::class.java),
                obj["polyline"]?.toString()
            )
        } ?: Line("000000", 0, "none", "none", 0, null, null, arrayOf(), "")
    }

}


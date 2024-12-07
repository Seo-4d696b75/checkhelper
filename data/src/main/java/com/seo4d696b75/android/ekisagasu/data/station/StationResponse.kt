package com.seo4d696b75.android.ekisagasu.data.station

import com.seo4d696b75.android.ekisagasu.data.database.station.StationEntity
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonObject

@Serializable
internal data class StationResponse(
    val id: String,
    val code: Int,
    val lat: Double,
    val lng: Double,
    val name: String,
    @SerialName("original_name")
    val originalName: String,
    @SerialName("name_kana")
    val nameKana: String,
    val prefecture: Int,
    val lines: List<Int>,
    val closed: Boolean,
    @Serializable(with = JsonObjectAsStringSerializer::class)
    val voronoi: String,
    val attr: String? = null,
) {
    fun toEntity() =
        StationEntity(id, code, lat, lng, name, originalName, nameKana, prefecture, lines, closed, voronoi, attr)
}

internal class JsonObjectAsStringSerializer : KSerializer<String> {
    override fun deserialize(decoder: Decoder): String {
        require(decoder is JsonDecoder)
        val obj = decoder.decodeJsonElement()
        require(obj is JsonObject)
        return obj.toString()
    }

    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("voronoi", PrimitiveKind.STRING)

    override fun serialize(
        encoder: Encoder,
        value: String,
    ): Unit = throw NotImplementedError()
}

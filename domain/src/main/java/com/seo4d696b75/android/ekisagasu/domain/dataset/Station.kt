package com.seo4d696b75.android.ekisagasu.domain.dataset

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

/**
 * @author Seo-4d696b75
 * @version 2020/12/17.
 */
@Serializable
data class Station(
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

    fun isLine(line: Line): Boolean {
        return lines.contains(line.code)
    }
}

@Serializable
data class StationRegistration(
    val code: Int,
    val numbering: List<String>? = null,
) {
    fun getNumberingString(): String {
        return numbering?.joinToString(separator = "/", transform = String::toString) ?: ""
    }
}

class JsonObjectAsStringSerializer : KSerializer<String> {
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

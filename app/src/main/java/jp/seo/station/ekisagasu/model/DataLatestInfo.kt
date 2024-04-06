package jp.seo.station.ekisagasu.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.text.StringCharacterIterator

@Serializable
data class DataLatestInfo(
    val version: Long,
    @SerialName("size")
    val length: Long,
) : java.io.Serializable {
    fun fileSize(): String {
        var bytes = length
        if (bytes < 0) return "0 B"
        if (bytes < 1000) return "$bytes B"
        val ci = StringCharacterIterator("KMGTPE")
        while (bytes >= 999_950) {
            bytes /= 1000
            ci.next()
        }
        return String.format("%.1f %cB", bytes.toFloat() / 1000.0f, ci.current())
    }
}

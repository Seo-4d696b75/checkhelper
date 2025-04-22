package com.seo4d696b75.android.ekisagasu.ui.navigation

import android.net.Uri
import android.os.Bundle
import androidx.navigation.NavType
import com.seo4d696b75.android.ekisagasu.domain.dataset.LatestDataVersion
import com.seo4d696b75.android.ekisagasu.domain.dataset.update.DataUpdateType
import com.seo4d696b75.android.ekisagasu.ui.log.LogOutputConfig
import com.seo4d696b75.android.ekisagasu.ui.selectLine.LineSelectType
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlin.reflect.KType
import kotlin.reflect.typeOf

val typeMap: Map<KType, @JvmSuppressWildcards NavType<*>> = mapOf(
    typeOf<DataUpdateType>() to NavType.EnumType(DataUpdateType::class.java),
    typeOf<LatestDataVersion>() to serializableNavType(LatestDataVersion.serializer(), false),
    typeOf<LineSelectType>() to NavType.EnumType(LineSelectType::class.java),
    typeOf<LogOutputConfig.Geo>() to serializableNavType(LogOutputConfig.Geo.serializer(), false),
)

private inline fun <reified T> serializableNavType(serializer: KSerializer<T>, nullable: Boolean) = object :
    NavType<T>(nullable) {
    override fun get(bundle: Bundle, key: String): T? = bundle.getString(key)?.let(::parseValue)

    override fun parseValue(value: String): T {
        val json = Uri.decode(value)
        return Json.decodeFromString(serializer, json)
    }

    override fun serializeAsValue(value: T): String {
        val json = Json.encodeToString(serializer, value)
        return Uri.encode(json)
    }

    override fun put(bundle: Bundle, key: String, value: T) {
        bundle.putString(key, serializeAsValue(value))
    }
}

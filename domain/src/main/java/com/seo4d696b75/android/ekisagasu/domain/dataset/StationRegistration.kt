package com.seo4d696b75.android.ekisagasu.domain.dataset

import kotlinx.serialization.Serializable

@Serializable
data class StationRegistration(
    val code: Int,
    val numbering: List<String>? = null,
) {
    fun getNumberingString(): String {
        return numbering?.joinToString(separator = "/", transform = String::toString) ?: ""
    }
}

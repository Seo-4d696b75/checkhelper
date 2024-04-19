package com.seo4d696b75.android.ekisagasu.ui.utils

/**
 * @author Seo-4d696b75
 * @version 2021/01/19.
 */

fun parseColorCode(value: String?): Int {
    val color = value?.substring(1)?.toInt(16) ?: 0xcccccc
    return color.or(0xff000000.toInt())
}

fun getVFromColorCode(value: String): Int {
    var color = value.substring(1).toInt(16)
    val b = color.and(0xff)
    color = color.ushr(8)
    val g = color.and(0xff)
    color = color.ushr(8)
    val r = color.and(0xff)
    return maxOf(r, g, b)
}

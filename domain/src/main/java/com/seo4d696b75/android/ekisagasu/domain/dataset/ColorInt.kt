package com.seo4d696b75.android.ekisagasu.domain.dataset

@JvmInline
value class ColorInt(val code: Int) {
    companion object {
        fun from(value: String?): ColorInt {
            val color = value?.substring(1)?.toInt(16) ?: 0xcccccc
            return ColorInt(color or 0xff000000.toInt())
        }
    }
}
